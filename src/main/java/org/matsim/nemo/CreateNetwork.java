package org.matsim.nemo;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.scenarioCreation.counts.CombinedCountsWriter;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;
import org.matsim.scenarioCreation.network.FineNetworkFilter;
import org.matsim.scenarioCreation.network.NetworkCreator;
import org.matsim.scenarioCreation.network.NetworkInput;
import org.matsim.scenarioCreation.network.SupplyOutput;
import org.matsim.scenarioCreation.pt.CreateScenarioFromGtfs;
import org.matsim.scenarioCreation.pt.CreateScenarioFromOsmFile;
import org.matsim.scenarioCreation.pt.PtInput;
import org.matsim.util.NEMOUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class CreateNetwork {

	private static final Logger logger = LoggerFactory.getLogger(CreateNetwork.class);

	public static void main(String[] args) {

		// parse the input variables
		InputArguments inputArguments = new InputArguments();
		JCommander.newBuilder().addObject(inputArguments).build().parse(args);
		SupplyOutput outputParams = new SupplyOutput(inputArguments.svnDir);
		NetworkInput inputParams = new NetworkInput(inputArguments.svnDir);

		Network network = createAndWriteNetwork(inputArguments.svnDir, inputArguments.scenario, inputParams, outputParams.getOutputNetworkDir());

		if (!ScenarioName.locationChoice.equals(inputArguments.scenario)) {

			logger.info("Creating public transit from osm schedule and gtfs data");
			Network transitNetwork = createAndWriteTransit(inputArguments.svnDir, inputArguments.scenario, outputParams.getOutputNetworkDir());
			MergeNetworks.merge(network, "pt_", transitNetwork);
		}

		if (ScenarioName.healthyCity.equals(inputArguments.scenario)) {
			banCarfromResidentialAreasAndCreateBikeLinks(network, inputParams.getInputNetworkShapeFilter());
		}

		new NetworkWriter(network).write(outputParams.getOutputNetworkDir()
				.resolve(inputArguments.scenario)
				.resolve("nemo_" + inputArguments.scenario + "_network.xml.gz").toString());

		createAndWriteCounts(inputArguments.svnDir,
				inputArguments.scenario,
				network,
				inputParams.getInputNetworkShapeFilter(),
				outputParams.getOutputNetworkDir());


		else if (ScenarioName.smartCity.equals(inputArguments.scenario)) {
			// do smart city stuff
		}

	}

	private static Network createAndWriteNetwork(String svnDir, String scenarioName, NetworkInput inputParams, Path outputDir) {

		// create the network
		NetworkCreator creator = new NetworkCreator.Builder()
				.setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
				.setSvnDir(svnDir)
				.withByciclePaths()
				.withRideOnCarLinks()
				.withOsmFilter(getNetworkFilter(scenarioName, inputParams.getInputNetworkShapeFilter()))
				.withCleaningModes(TransportMode.car, TransportMode.ride, TransportMode.bike)
				.withRideOnCarLinks()
				.build();

		Network network = creator.createNetwork();

		if (ScenarioName.bikeHighways.equals(scenarioName)) {
			logger.info("Scenario with bike highways. Merging bike highways into network");
			Network highwayInput = NetworkUtils.createNetwork();
			new MatsimNetworkReader(highwayInput).readFile(inputParams.getInputBikeHighwayNetwork());
			BikeNetworkMerger merger = new BikeNetworkMerger(network);
			network = merger.mergeBikeHighways(highwayInput);
		}


		return network;
	}

	private static Network createAndWriteTransit(String svnDir, String scenarioName, Path outputDir) {

		PtInput ptInputParams = new PtInput(svnDir);

		// read in transit-network, vehicles and schedule from osm
		Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile().run(ptInputParams.getOsmScheduleFile().toString());

		// read in transit-network, vehicles and schedule from gtfs
		Scenario scenarioFromGtfsSchedule = new CreateScenarioFromGtfs().run(ptInputParams.getGtfsFile().toString());

		// merge two transit networks into gtfs network
		MergeNetworks.merge(scenarioFromGtfsSchedule.getNetwork(), "", scenarioFromOsmSchedule.getNetwork());
		mergeSchedules(scenarioFromGtfsSchedule.getTransitSchedule(), scenarioFromOsmSchedule.getTransitSchedule());
		mergeVehicles(scenarioFromGtfsSchedule.getTransitVehicles(), scenarioFromOsmSchedule.getTransitVehicles());

		logger.info("Writing transit-vehicles and schedule");
		new VehicleWriterV1(scenarioFromGtfsSchedule.getTransitVehicles())
				.writeFile(outputDir
						.resolve(scenarioName)
						.resolve("nemo_" + scenarioName + "_transit-vehicles.xml.gz")
						.toString());
		new TransitScheduleWriterV2(scenarioFromGtfsSchedule.getTransitSchedule())
				.write(outputDir
						.resolve(scenarioName)
						.resolve("nemo_" + scenarioName + "_transit-schedule.xml.gz")
						.toString());
		return scenarioFromGtfsSchedule.getNetwork();
	}

	private static void createAndWriteCounts(String svnDir, String scenarioName, Network network, String filterShape, Path outputDir) {
		// create long term counts
		Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
		NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
				.setSvnDir(svnDir)
				.withNetwork(network)
				.withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 50025L)
				.useCountsWithinGeometry(filterShape)
				.build();
		Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

		// create short term counts
		NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
				.setSvnDir(svnDir)
				.withNetwork(network)
				.withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 5025L)
				.useCountsWithinGeometry(filterShape)
				.build();
		Map<String, Counts<Link>> shortTermCounts = shortTermCountsCreator.run();

		CombinedCountsWriter.writeCounts(outputDir
						.resolve(scenarioName), scenarioName,
				columnCombinations, longTermCounts, shortTermCounts);
	}

	private static OsmNetworkReader.OsmFilter getNetworkFilter(String scenario, String pathToShapeFile) {
		if (ScenarioName.locationChoice.equals(scenario))
			return (coord, level) -> (level <= 4);
		else
			return new FineNetworkFilter(pathToShapeFile);
	}

	private static void mergeSchedules(TransitSchedule schedule, TransitSchedule toBeMerged) {
		toBeMerged.getFacilities().values().forEach(schedule::addStopFacility);
		toBeMerged.getTransitLines().values().forEach(schedule::addTransitLine);
	}

	private static void mergeVehicles(Vehicles vehicles, Vehicles toBeMerged) {
		toBeMerged.getVehicleTypes().values().forEach(vehicles::addVehicleType);
		toBeMerged.getVehicles().values().forEach(vehicles::addVehicle);
	}

	private static void banCarfromResidentialAreasAndCreateBikeLinks(Network network, String shpFile) {

		List<Geometry> geometries = new ArrayList<>();
		ShapeFileReader.getAllFeatures(shpFile)
				.forEach(feature -> geometries.add((Geometry) feature.getDefaultGeometry()));

		NetworkFactory factory = network.getFactory();
		List<Link> newBikeLinks = new ArrayList<>();

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

			} else { // all non-residential links and residential outside of shape --> bike capacity

				double capacity = link.getCapacity();

				// if capacity > 1000. extra bike link, if lower no bike link

				if (capacity > 1000.) {
					// new bike link
					Id<Link> bikeLinkId = Id.createLinkId(link.getId() + "_bike");
					Link bikeLink = factory.createLink(bikeLinkId, link.getFromNode(), link.getToNode());
					Set<String> allowedModes = new HashSet<>();
					allowedModes.add(TransportMode.bike);
					bikeLink.setAllowedModes(allowedModes);
					bikeLink.setLength(link.getLength());
					bikeLink.setFreespeed(link.getFreespeed());
					bikeLink.setNumberOfLanes(1);

					Map<String, Object> attributes = link.getAttributes().getAsMap();
					for (Map.Entry<String, Object> entry : attributes.entrySet()) {
						bikeLink.getAttributes().putAttribute(entry.getKey(), entry.getValue());
					}

					// set capacity in new bike link and old link according to Copenhagen model
					if (capacity < 2000) {
						// 1000<cap<2000
						bikeLink.setCapacity(capacity - 1000.);
						link.setCapacity(1000);

					} else {
						// 2000<cap
						bikeLink.setCapacity(1000);
						link.setCapacity(capacity - 1000.);
					}

					newBikeLinks.add(bikeLink);

					// remove bike from old link
					Set<String> oldAllowedModes = link.getAllowedModes();
					Set<String> newAllowedModes = new HashSet<>();
					for (String mode : oldAllowedModes) {
						if (!mode.equals(TransportMode.bike)) {
							newAllowedModes.add(mode);
						}
					}
					link.setAllowedModes(newAllowedModes);
				}
			}
		}
		for (Link bikeLink : newBikeLinks) {
			network.addLink(bikeLink);
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

	private static class InputArguments {

		@Parameter(names = "-svnDir", required = true)
		private String svnDir;

		@Parameter(names = "-scenario", required = true)
		private String scenario;
	}

	private static class ScenarioName {
		private static String smartCity = "smartCity";
		private static String baseCase = "baseCase";
		private static String locationChoice = "locationChoice";
		private static String bikeHighways = "bikeHighways";
		private static String healthyCity = "healthyCity";
	}
}
