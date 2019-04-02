package org.matsim.scenarioCreation.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.scenarioCreation.counts.CombinedCountsWriter;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;
import org.matsim.scenarioCreation.pt.CreateScenarioFromGtfs;
import org.matsim.scenarioCreation.pt.CreateScenarioFromOsmFile;
import org.matsim.scenarioCreation.pt.PtInput;
import org.matsim.scenarioCreation.pt.PtOutput;
import org.matsim.util.NEMOUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/*
 * Creates the network and transit schedule+vehicles for Nemo Scenario 2
 * - fine network
 * - with car counts
 * - without bike only
 * - without bike highway
 * - cars banned from residential areas within Ruhrgebiet boundary shape
 */
public class CreateFineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadtOnlyCarBan {

	private static final String SUBDIR = "fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadtOnlyCarBan";
	private static final String FILE_PREFIX = "fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadtOnlyCarBan";
	private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadtOnlyCarBan.class);

	public static void main(String[] args) throws IOException {

		// parse input
		InputArguments arguments = new InputArguments();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		NetworkOutput outputParams = new NetworkOutput(arguments.svnDir);
		NetworkInput inputParams = new NetworkInput(arguments.svnDir);
		PtInput ptInputParams = new PtInput(arguments.svnDir);
		PtOutput ptOutputParams = new PtOutput(arguments.svnDir);

		// ensure all output directories are present
		Files.createDirectories(outputParams.getOutputNetworkDir().resolve(SUBDIR));

		// read in transit-network, vehicles and schedule from osm and double the number of departures
		Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile()
				.run(ptInputParams.getOsmScheduleFile().toString());

		// read in transit-network, vehicles and schedule from gtfs
		Scenario scenarioFromGtfsSchedule = new CreateScenarioFromGtfs().run(ptInputParams.getGtfsFile().toString());

		// merge two transit networks into gtfs network
		MergeNetworks.merge(scenarioFromGtfsSchedule.getNetwork(), "", scenarioFromOsmSchedule.getNetwork());
		mergeSchedules(scenarioFromGtfsSchedule.getTransitSchedule(), scenarioFromOsmSchedule.getTransitSchedule());
		mergeVehicles(scenarioFromGtfsSchedule.getTransitVehicles(), scenarioFromOsmSchedule.getTransitVehicles());

		new VehicleWriterV1(scenarioFromGtfsSchedule.getTransitVehicles())
				.writeFile(ptOutputParams.getTransitVehiclesFile().toString());
		new TransitScheduleWriterV2(scenarioFromGtfsSchedule.getTransitSchedule())
				.write(ptOutputParams.getTransitScheduleFile().toString());

		final String[] cleaningModesArray = {TransportMode.car, TransportMode.ride, TransportMode.bike};
		
		// create the network from scratch
		NetworkCreator creator = new NetworkCreator.Builder().setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
				.setSvnDir(arguments.svnDir)
				.withByciclePaths()
				.withOsmFilter(new FineNetworkFilterWithoutBikeOnlyLinks(inputParams.getInputNetworkShapeFilter()))
				.withCleaningModes(cleaningModesArray)
				.withRideOnCarLinks()
				.build();
		Network network = creator.createNetwork();
		
		banCarfromResidentialAreasAndCreateBikeLinks(network, inputParams.getInputNetworkShapeFilter());
		for (String mode : new HashSet<>(Arrays.asList(cleaningModesArray))) {
			new MultimodalNetworkCleaner(network).run(new HashSet<>(Collections.singletonList(mode)));
		}
		
		logger.info("merge transit networks and car/ride/bike network");
		MergeNetworks.merge(network, "", scenarioFromGtfsSchedule.getNetwork());

		final Path outputNetwork = outputParams.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + ".xml.gz");
		logger.info("writing network to: " + outputNetwork.toString());
		new NetworkWriter(network).write(outputNetwork.toString());

		// create long term counts
		Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
		NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
				.setSvnDir(arguments.svnDir).withNetwork(network).withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 50025L).build();
		Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

		// create short term counts
		NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
				.setSvnDir(arguments.svnDir).withNetwork(network).withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 5025L).build();
		Map<String, Counts<Link>> shortTermCounts = shortTermCountsCreator.run();

		CombinedCountsWriter.writeCounts(outputParams.getOutputNetworkDir().resolve(SUBDIR), FILE_PREFIX,
				columnCombinations, longTermCounts, shortTermCounts);

	}

	private static void mergeSchedules(TransitSchedule schedule, TransitSchedule toBeMerged) {
		toBeMerged.getFacilities().values().forEach(schedule::addStopFacility);
		toBeMerged.getTransitLines().values().forEach(schedule::addTransitLine);
	}

	private static void mergeVehicles(Vehicles vehicles, Vehicles toBeMerged) {
		toBeMerged.getVehicleTypes().values().forEach(vehicles::addVehicleType);
		toBeMerged.getVehicles().values().forEach(vehicles::addVehicle);
	}

	private static class InputArguments {

		@Parameter(names = "-svnDir", required = true, description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
		private String svnDir;
	}

	private static void banCarfromResidentialAreasAndCreateBikeLinks(Network network, String shpFile) {

		List<Geometry> geometries = new ArrayList<>();
		ShapeFileReader.getAllFeatures(shpFile)
				.forEach(feature -> geometries.add((Geometry) feature.getDefaultGeometry()));

		for (Link link : network.getLinks().values()) {
			if (link.getAttributes().getAttribute("type").equals("residential") && linkIsInShape(link, geometries)) {
				// link is residential and in shapeFile --> ban cars
				Set<String> oldAllowedModes = link.getAllowedModes();
				Set<String> newAllowedModes = new HashSet<>();
				for (String mode : oldAllowedModes) {
					if (!mode.equals("car") && !mode.equals("ride")) {
						newAllowedModes.add(mode);
					}
				}
				link.setAllowedModes(newAllowedModes);

			}
		}
	}

	private static boolean linkIsInShape(Link link, List<Geometry> geometries) {

		Coord fromCoord = link.getFromNode().getCoord();
		Coord toCoord = link.getToNode().getCoord();
		boolean fromCoordInShape = geometries.stream()
				.anyMatch(geometry -> geometry.contains(MGC.coord2Point(fromCoord)));
		boolean toCoordInShape = geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(toCoord)));

		return fromCoordInShape && toCoordInShape;

	}

}
