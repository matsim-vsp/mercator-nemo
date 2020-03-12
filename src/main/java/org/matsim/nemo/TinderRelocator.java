package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TinderRelocator {

	private static final String BASE_CASE_CONFIG = "projects\\nemo_mercator\\data\\matsim_input\\baseCase\\config_baseCase.xml";
	private static final String MURMO_SHAPE_FILE = "projects\\nemo_mercator\\data\\original_files\\murmo\\Ruhr_Grid_1km\\Ruhr_Grid_1km_EW.shp";
	private static final String MURMO_TRANSITION_DATA = "projects\\nemo_mercator\\data\\original_files\\murmo\\p_anti.csv";
	private static final Comparator<SimpleFeature> featureComparator = Comparator.comparingLong(feature -> (Long) (feature.getAttribute("ID_Gitter_")));
	private static final String WAS_MOVED_KEY = "was_moved";

	private static final Logger logger = Logger.getLogger(TinderRelocator.class);
	private static final Random random = MatsimRandom.getRandom();

	@Parameter(names = {"-sharedSvn"}, required = true)
	private String sharedSvn;

	@Parameter(names = {"-runsSvn"}, required = true)
	private String runsSvn;

	@Parameter(names = {"-scalingFactor", "-sf"})
	private double scalingFactor = 0.01;

	@Parameter(names = {"-tinderMatches", "-tm"})
	private double shareOfTinderMatches = 0.5;

	private Scenario scenario;
	private List<SimpleFeature> murmoFeatures;
	private QuadTree<Id<Person>> spatialPersonIndex;

	private int movedPersonCounter = 0;
	private int emptySourceCellCounter = 0;
	private double sumOfPeopleToMove;


	public static void main(String[] args) throws IOException {

		TinderRelocator relocator = new TinderRelocator();
		JCommander.newBuilder().addObject(relocator).build().parse(args);
		relocator.run();
	}

	private void run() throws IOException {

		final Path sharedSvnPath = Paths.get(sharedSvn);
		final Path runsSvnPath = Paths.get(runsSvn);

		// read in the base case scenario
		//scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(sharedSvnPath.resolve(BASE_CASE_CONFIG).toString()));
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new PopulationReader(scenario).readFile(runsSvnPath.resolve("nemo\\baseCaseCalibration2\\baseCase_021\\output\\baseCase_021.output_plans.xml.gz").toString());

		// remove all plans other than the selected plan
		// also remove all route informations
		scenario.getPopulation().getPersons().values().parallelStream()
				.forEach(person -> {

					Plan selectedPlan = person.getSelectedPlan();
					person.getPlans().clear();
					person.addPlan(selectedPlan);
					person.setSelectedPlan(selectedPlan);

					person.getSelectedPlan().getPlanElements().stream()
							.filter(element -> element instanceof Leg)
							.map(element -> (Leg) element)
							.forEach(leg -> leg.setRoute(null));
				});

		// get the feature source of the murmo grid, to extract bounds from it
		SimpleFeatureSource featureSource = ShapeFileReader.readDataFile(sharedSvnPath.resolve(MURMO_SHAPE_FILE).toString());

		// create a spacial index of agents
		initializeSpatialIndex(scenario, featureSource.getBounds());

		// read in murmo transition raster
		murmoFeatures = ShapeFileReader.getAllFeatures(sharedSvnPath.resolve(MURMO_SHAPE_FILE).toString()).stream()
				.sorted(featureComparator)
				.collect(Collectors.toList());

		// iterate over mumo csv
		try (FileReader reader = new FileReader(sharedSvnPath.resolve(MURMO_TRANSITION_DATA).toString())) {

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
					Object wasMovedRaw = person.getAttributes().getAttribute(WAS_MOVED_KEY);
					return wasMovedRaw != null;
				})
				.collect(Collectors.toList());

		Map<Integer, List<Person>> groupByTimesMoved = movedAgents.stream()
				.collect(Collectors.groupingBy(person -> (int) person.getAttributes().getAttribute(WAS_MOVED_KEY)));

		var movedAllActivities = movedAgents.stream()
				.filter(person -> {
					Object movedAllActivitiesRaw = person.getAttributes().getAttribute("moved-all-activities");
					return movedAllActivitiesRaw != null;
				})
				.collect(Collectors.toList());

		logger.info("moved agents size: " + movedAgents.size());
		logger.info("moved all activities: " + movedAllActivities.size());
		logger.info("moved counter: " + movedPersonCounter);
		logger.info("empty cells: " + emptySourceCellCounter);

		for (Map.Entry<Integer, List<Person>> integerListEntry : groupByTimesMoved.entrySet()) {
			logger.info(integerListEntry.getValue().size() + " persons moved " + integerListEntry.getKey() + " times");
		}

		String filename = "population_" + shareOfTinderMatches + "_matched-agents.xml.gz";
		// write out new population
		new PopulationWriter(scenario.getPopulation()).write(sharedSvnPath.resolve("projects\\nemo_mercator\\data\\matsim_input\\deurbanisation").resolve(filename).toString());

		// write out new popoulation as csv
		try (CSVPrinter printer = CSVFormat.DEFAULT.withHeader("id", "homeX", "homeY", "was-moved", "moved-all-activities")
				.print(sharedSvnPath.resolve("projects\\nemo_mercator\\data\\matsim_input\\deurbanisation\\population.csv"), Charset.defaultCharset())) {

			for (Person p : scenario.getPopulation().getPersons().values()) {
				Optional<Activity> homeActivity = p.getSelectedPlan().getPlanElements().stream()
						.filter(element -> element instanceof Activity)
						.map(element -> (Activity) element)
						.filter(activity -> activity.getType().startsWith("home"))
						.findAny();

				if (!homeActivity.isPresent()) throw new RuntimeException("no!");
				Activity home = homeActivity.get();
				printer.printRecord(p.getId(), home.getCoord().getX(), home.getCoord().getY(), p.getAttributes().getAttribute(WAS_MOVED_KEY), p.getAttributes().getAttribute("moved-all-activities"));
			}
		}
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

		Double baseInhabitants = (Double) sourceFeature.getAttribute("EW_2011");
		Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
		List<Id<Person>> peopleWithinSource = getPersonsFromSpatialIndex(geometry);

		//logger.info("#" + index + " baseInhabitants=" + baseInhabitants + ", sourceInhabitants=" + peopleWithinSource.size());

		double rowSum = 0;
		for (int columnIndex = 0; columnIndex < record.size(); columnIndex++) {

			// the we just need the lower half of the matrix since it is anti-symmetrical now
			if (columnIndex == index) {
				break;
			}

			// the destination feature is the index of the colum
			SimpleFeature destinationFeature = murmoFeatures.get(columnIndex);

			// number of people moving from cell (index) to cell (columnIndex)
			double value = Double.parseDouble(record.get(columnIndex));
			rowSum += value;

			if (value >= 0)
				move(sourceFeature, destinationFeature, value);
			else
				move(destinationFeature, sourceFeature, value);
		}
		//	logger.info("# " + index + " is: " + rowSum);
	}

	private void move(SimpleFeature sourceFeature, SimpleFeature destinationFeature, double value) {

		// get all the agents from the source feature
		Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
		Double baseInhabitants = (Double) sourceFeature.getAttribute("EW_2011");
		List<Id<Person>> peopleWithinSource = getPersonsFromSpatialIndex(geometry);

		if (peopleWithinSource.size() > 0) {

			double shareOfPeopleMoving = Math.abs(value * 20) / baseInhabitants;
			List<Person> matsimPeopleMoving = peopleWithinSource.stream()
					.map(id -> scenario.getPopulation().getPersons().get(id))
					.filter(person -> person.getAttributes().getAttribute(WAS_MOVED_KEY) == null)
					.filter(person -> random.nextDouble() < shareOfPeopleMoving)
					.collect(Collectors.toList());

			if (matsimPeopleMoving.size() > 0) {
				logger.info("moving " + matsimPeopleMoving.size() + " people. Share=" + shareOfPeopleMoving + " base=" + baseInhabitants + " value=" + value);
			}

			for (Person person : matsimPeopleMoving) {
				movePerson(person, (Geometry) destinationFeature.getDefaultGeometry());
				person.getAttributes().putAttribute("source-feature", sourceFeature.getAttribute("ID_Gitter_"));
				person.getAttributes().putAttribute("destination-feature", destinationFeature.getAttribute("ID_Gitter_"));
			}

		} else {
			emptySourceCellCounter++;
		}
	}

	private void movePerson(Person movingPerson, Geometry destinationGeometry) {

		// this only moves the home activities for now. Later we should also move other stuff.
		List<Activity> homeActivities = movingPerson.getSelectedPlan().getPlanElements().stream()
				.filter(element -> element instanceof Activity)
				.map(element -> (Activity) element)
				.filter(activity -> activity.getType().startsWith("home"))
				.collect(Collectors.toList());

		//Coord newHomeCoord = drawCoordFromGeometry((Geometry) murmoFeatures.get(10).getDefaultGeometry());
		Coord newHomeCoord = drawCoordFromGeometry(destinationGeometry);
		boolean isSpatialIndexUpdated = false;

		for (Activity homeActivity : homeActivities) {
			if (!isSpatialIndexUpdated) {
				spatialPersonIndex.remove(homeActivity.getCoord().getX(), homeActivity.getCoord().getY(), movingPerson.getId());
				spatialPersonIndex.put(newHomeCoord.getX(), newHomeCoord.getY(), movingPerson.getId());
				isSpatialIndexUpdated = true;
			}

			homeActivity.setCoord(newHomeCoord);
		}


		Object wasMovedAttribute = movingPerson.getAttributes().getAttribute(WAS_MOVED_KEY);
		if (wasMovedAttribute == null) {
			movingPerson.getAttributes().putAttribute(WAS_MOVED_KEY, 1);
		} else {
			int timesMoved = (int) wasMovedAttribute + 1;
			movingPerson.getAttributes().putAttribute(WAS_MOVED_KEY, timesMoved);
		}
		movedPersonCounter++;

		// people who are not a tinder match relocate their other activities as well
		// all tinder matches move with somebody else but keep their remaining activity context
		if (random.nextDouble() > shareOfTinderMatches) {
			List<Activity> nonHomeActivities = movingPerson.getSelectedPlan().getPlanElements().stream()
					.filter(element -> element instanceof Activity)
					.map(element -> (Activity) element)
					.filter(activity -> !activity.getType().startsWith("home"))
					.collect(Collectors.toList());

			for (Activity activity : nonHomeActivities) {

				// assuming that all activities are within the same square
				Coord newActivityLocation = drawCoordFromGeometry(destinationGeometry);
				activity.setCoord(newActivityLocation);
			}
			movingPerson.getAttributes().putAttribute("moved-all-activities", 1);
		}
	}

	private void handleHeaderRecord(CSVRecord record) {
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
