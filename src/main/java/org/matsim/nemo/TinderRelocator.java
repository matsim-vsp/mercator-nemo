package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TinderRelocator {

	private static final String BASE_CASE_CONFIG = "projects\\nemo_mercator\\data\\matsim_input\\baseCase\\config_baseCase.xml";
	private static final String MURMO_SHAPE_FILE = "projects\\nemo_mercator\\data\\original_files\\murmo\\Ruhr_Grid_1km\\Ruhr_Grid_1km_EW.shp";
	private static final String MURMO_TRANSITION_DATA = "projects\\nemo_mercator\\data\\original_files\\murmo\\p_trans.csv";
	private static final Comparator<SimpleFeature> featureComparator = Comparator.comparingLong(feature -> (Long) (feature.getAttribute("ID_Gitter_")));

	private static final Logger logger = Logger.getLogger(TinderRelocator.class);

	@Parameter(names = {"-svnDir"}, required = true)
	private String svnDir;

	private Scenario scenario;
	private List<SimpleFeature> murmoFeatures;
	private QuadTree<Id<Person>> spatialPersonIndex;

	private int movedPersonCounter = 0;
	private int emptySourceCellCounter = 0;

	public static void main(String[] args) throws IOException {

		TinderRelocator relocator = new TinderRelocator();
		JCommander.newBuilder().addObject(relocator).build().parse(args);
		relocator.run();
	}

	private void run() throws IOException {

		final Path svnPath = Paths.get(svnDir);

		// read in the base case scenario
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(svnPath.resolve(BASE_CASE_CONFIG).toString()));

		// get the feature source of the murmo grid, to extract bounds from it
		SimpleFeatureSource featureSource = ShapeFileReader.readDataFile(svnPath.resolve(MURMO_SHAPE_FILE).toString());

		// create a spacial index of agents
		initializeSpatialIndex(scenario, featureSource.getBounds());

		// read in murmo transition raster
		murmoFeatures = ShapeFileReader.getAllFeatures(svnPath.resolve(MURMO_SHAPE_FILE).toString()).stream()
				.sorted(featureComparator)
				.collect(Collectors.toList());

		// iterate over mumo csv
		try (FileReader reader = new FileReader(svnPath.resolve(MURMO_TRANSITION_DATA).toString())) {

			for (CSVRecord record : CSVFormat.newFormat(',').parse(reader)) {
				if (record.getRecordNumber() == 1)
					handleHeaderRecord(record);
				else {
					handleRecord(record);
				}
			}
		}

		List<Person> movedAgents = scenario.getPopulation().getPersons().values().stream()
				.filter(person -> {
					Object wasMovedRaw = person.getAttributes().getAttribute("was_moved");
					return wasMovedRaw != null;
				})
				.collect(Collectors.toList());

		logger.info("moved agents size: " + movedAgents.size());
		logger.info("moved counter: " + movedPersonCounter);
		logger.info("empty cells: " + emptySourceCellCounter);

		// write out new population
		new PopulationWriter(scenario.getPopulation()).write(Paths.get("G:\\Users\\Janek\\Desktop\\tinder-test\\population.xml.gz").toString());

	}

	private void initializeSpatialIndex(Scenario scenario, ReferencedEnvelope bounds) {

		spatialPersonIndex = new QuadTree<>(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());

		for (Person person : scenario.getPopulation().getPersons().values()) {

			Optional<Activity> homeActivity = person.getSelectedPlan().getPlanElements().stream()
					.filter(element -> element instanceof Activity)
					.map(element -> (Activity) element)
					.filter(activity -> activity.getType().startsWith("home"))
					.filter(activity -> isWithinBounds(activity.getCoord()))
					.findAny();

			homeActivity.ifPresent(activity ->
					spatialPersonIndex.put(activity.getCoord().getX(), activity.getCoord().getY(), person.getId()));
		}
	}

	private void handleRecord(CSVRecord record) {

		// index is record number - 2, since first row is header and record number is 1-index based
		final long index = record.getRecordNumber() - 2;

		// get the source shape
		SimpleFeature sourceFeature = murmoFeatures.get((int) index);

		for (int columnIndex = 0; columnIndex < record.size(); columnIndex++) {

			// we have a symmetric matrix only parse the lower half
			if (columnIndex >= index) {
				break;
			}

			// the destination feature is the index of the colum
			SimpleFeature destinationFeature = murmoFeatures.get(columnIndex);

			// propability of moving from cell (index) to cell (columnIndex)
			double value = Double.parseDouble(record.get(columnIndex));

			if (value >= 0)
				move(sourceFeature, destinationFeature, value);
			else
				move(destinationFeature, sourceFeature, value);
		}
	}

	private void move(SimpleFeature sourceFeature, SimpleFeature destinationFeature, double value) {

		// get all the agents from the source feature
		Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
		Double baseInhabitants = (Double) sourceFeature.getAttribute("EW_2011");
		List<Id<Person>> peopleWithinSource = getPersonsFromSpatialIndex(geometry);

		if (peopleWithinSource.size() > 0) {
			long numberOfPeopleToMove = Math.round(baseInhabitants * Math.abs(value) * 0.01); // use scaling factor of 1% for now. Make it configurable later
			movePersons(numberOfPeopleToMove, peopleWithinSource, destinationFeature);
		} else {
			emptySourceCellCounter++;
		}
	}

	private void handleHeaderRecord(CSVRecord record) {
	}

	private void movePersons(long numberOfPersonsToMove, List<Id<Person>> personsWithinSourceCell, SimpleFeature destinationFeature) {

		for (int i = 0; i < numberOfPersonsToMove && i < personsWithinSourceCell.size(); i++) {
			Id<Person> id = personsWithinSourceCell.get(i);
			Person person = scenario.getPopulation().getPersons().get(id);
			Activity homeActivity = person.getSelectedPlan().getPlanElements().stream()
					.filter(element -> element instanceof Activity)
					.map(element -> (Activity) element)
					.filter(activity -> activity.getType().startsWith("home"))
					.findAny()
					.orElseThrow(() -> new RuntimeException("no one should be homeless"));

			//Coord newHomeCoord = drawCoordFromGeometry((Geometry) destinationFeature.getDefaultGeometry());
			Coord newHomeCoord = drawCoordFromGeometry((Geometry) murmoFeatures.get(10).getDefaultGeometry());
			homeActivity.setCoord(newHomeCoord);
			person.getAttributes().putAttribute("was_moved", true);
			movedPersonCounter++;
		}
	}

	private boolean isWithinBounds(Coord homeCoord) {
		return spatialPersonIndex.getMinEasting() < homeCoord.getX() && spatialPersonIndex.getMaxEasting() > homeCoord.getX()
				&& spatialPersonIndex.getMinNorthing() < homeCoord.getY() && spatialPersonIndex.getMaxNorthing() > homeCoord.getY();
	}

	private List<Id<Person>> getPersonsFromSpatialIndex(Geometry geometry) {

		List<Id<Person>> result = new ArrayList<>();
		Envelope envelope = geometry.getEnvelopeInternal();
		spatialPersonIndex.getRectangle(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), result);
		return result;
	}

	private Coord drawCoordFromGeometry(Geometry geometry) {

		Envelope envelope = geometry.getEnvelopeInternal();
		double x = envelope.getMinX() + Math.random() * envelope.getWidth();
		double y = envelope.getMinY() + Math.random() * envelope.getHeight();
		return new Coord(x, y);
	}
}
