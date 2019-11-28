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
import org.matsim.nemo.counts.*;
import org.matsim.nemo.pt.CreatePtScheduleAndVehiclesFromGtfs;
import org.matsim.nemo.pt.CreatePtScheduleAndVehiclesFromOsm;
import org.matsim.nemo.pt.PtInput;
import org.matsim.nemo.util.NEMOUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Script to create all the supply (network, counts, transit-vehicles and schedules). It takes the svn-directory and the
 * scenario name as parameters. Possible scenario names can be found in {@link CreateSupply.ScenarioName}. If you want
 * to add another scenario add the name and the validation in that class as well and then put switches into the scipt
 * <p>
 * All the ouptut is written to <path-to-your-svn>/projects/nemo_mercator/data/matsim_input/supply/<scenarioName>
 * <p>
 * The program can be started with the following arguments -svnDir <path-to-your-svn> -scenario <scenarioName>
 */
public class CreateSupply {

	private static Logger logger = Logger.getLogger("CreateSupply");

	public static void main(String[] args) throws IOException {

		// parse the input variables
		InputArguments inputArguments = new InputArguments();
		JCommander.newBuilder().addObject(inputArguments).build().parse(args);
		validateInput(inputArguments);

		SupplyOutput outputParams = new SupplyOutput(inputArguments.svnDir);
		NetworkInput inputParams = new NetworkInput(inputArguments.svnDir);

		// ensure output directory is present
		Files.createDirectories(outputParams.getOutputNetworkDir().resolve(inputArguments.scenario));

		// parse the osm network
		Network network = createNetwork(inputArguments.svnDir, inputArguments.scenario, inputParams);

		// if we are preparing the network for a scenario other than location choice we need a public transit network
		if (!ScenarioName.locationChoice.equals(inputArguments.scenario)) {

			logger.info("Creating public transit from osm schedule and gtfs data");
			Network transitNetwork = createAndWriteTransit(inputArguments.svnDir, inputArguments.scenario, outputParams.getOutputNetworkDir());
			MergeNetworks.merge(network, "pt_", transitNetwork);
		}

		// write the network to the output folder
		new NetworkWriter(network).write(outputParams.getOutputNetworkDir()
				.resolve(inputArguments.scenario)
				.resolve("nemo_" + inputArguments.scenario + "_network.xml.gz").toString());

		// create counts for calibration
		createAndWriteCounts(inputArguments.svnDir,
				inputArguments.scenario,
				network,
				inputParams.getInputNetworkShapeFilter(),
				outputParams.getOutputNetworkDir());
	}

	private static void validateInput(InputArguments inputArguments) {
		if (!ScenarioName.isValidScenarioName(inputArguments.scenario)) {
			throw new RuntimeException("Invalid scenario name! Possible options are " + ScenarioName.getAllNames());
		}

		// make sure everything is available
		NetworkInput inputParams = new NetworkInput(inputArguments.svnDir);
		if (Files.notExists(Paths.get(inputParams.getInputBikeHighwayNetwork())))
			throw new RuntimeException(Paths.get(inputParams.getInputBikeHighwayNetwork()).toString() + " does not exist");

		if (Files.notExists(Paths.get(inputParams.getInputBikeHighwayNetworkWithBridge())))
			throw new RuntimeException(Paths.get(inputParams.getInputBikeHighwayNetworkWithBridge()).toString() + " does not exist");

		if (Files.notExists(Paths.get(inputParams.getInputNetworkShapeFilter())))
			throw new RuntimeException(Paths.get(inputParams.getInputNetworkShapeFilter()).toString() + " does not exist");

		CountsInput countsInput = new CountsInput(inputArguments.svnDir);
		if (Files.notExists(Paths.get(countsInput.getInputLongtermCountDataRootDir())))
			throw new RuntimeException(Paths.get(countsInput.getInputLongtermCountDataRootDir()).toString() + "does not exist");

		if (Files.notExists(Paths.get(countsInput.getInputLongtermCountNodesMapping())))
			throw new RuntimeException(Paths.get(countsInput.getInputLongtermCountNodesMapping()).toString() + "does not exist");

		if (Files.notExists(Paths.get(countsInput.getInputShorttermCountDataRootDir())))
			throw new RuntimeException(Paths.get(countsInput.getInputShorttermCountDataRootDir()).toString() + "does not exist");

		if (Files.notExists(Paths.get(countsInput.getInputShorttermCountMapping())))
			throw new RuntimeException(Paths.get(countsInput.getInputShorttermCountMapping()).toString() + "does not exist");
	}

	private static Network createNetwork(String svnDir, String scenarioName, NetworkInput inputParams) {

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

		logger.info("Creating network. This may take some while");
		Network network = creator.createNetwork();

		if (ScenarioName.healthyCity.equals(scenarioName)) {
			logger.info("Banning cars from residential areas, as part of the healthy city scenario");
			banCarfromResidentialAreasAndCreateBikeLinks(network, inputParams.getInputNetworkShapeFilter());
		}

		if (ScenarioName.bikeHighways.equals(scenarioName) || ScenarioName.bikeHighwaysWithBridge.equals(scenarioName)) {

			Network highwayInput = NetworkUtils.createNetwork();

			String inputNetwork;
			if (ScenarioName.bikeHighways.equals(scenarioName)) {
				logger.info("Scenario with bike highways. Merging bike highways into network");
				inputNetwork = inputParams.getInputBikeHighwayNetwork();
			} else {
				logger.info("Scenario with bike highways and bridge. So merging bike highways into network");
				inputNetwork = inputParams.getInputBikeHighwayNetworkWithBridge();
			}

			new MatsimNetworkReader(highwayInput).readFile(inputNetwork);
			BikeNetworkMerger merger = new BikeNetworkMerger(network);
			network = merger.mergeBikeHighways(highwayInput);
		}

		return network;
	}

	private static Network createAndWriteTransit(String svnDir, String scenarioName, Path outputDir) {

		PtInput ptInputParams = new PtInput(svnDir);

		// if the sceario is healthy city we want to double the pt supply
		double headwayFactor = scenarioName.equals(ScenarioName.healthyCity) ? 0.5 : 1.0;

		// read in transit-network, vehicles and schedule from osm
		Scenario scenarioFromOsmSchedule = new CreatePtScheduleAndVehiclesFromOsm().run(ptInputParams.getOsmScheduleFile().toString(), headwayFactor);

		// read in transit-network, vehicles and schedule from gtfs
		Scenario scenarioFromGtfsSchedule = new CreatePtScheduleAndVehiclesFromGtfs().run(ptInputParams.getGtfsFile().toString());

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
		LongTermCountsCreator longTermCountsCreator = new LongTermCountsCreator.Builder()
				.setSvnDir(svnDir)
				.withNetwork(network)
				.withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 50025L)
				.useCountsWithinGeometry(filterShape)
				.build();
		Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

		// create short term counts
		ShortTermCountsCreator shortTermCountsCreator = new ShortTermCountsCreator.Builder()
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
		if (ScenarioName.locationChoice.equals(scenario)) {
			logger.info("Scenario location choice: Using OSM-Filter which includes all streets down ot level 4");
			return (coord, level) -> (level <= 4);
		} else {
			logger.info("Using FineNetworkFilter as OSM-Filter");
			return new FineNetworkFilter(pathToShapeFile);
		}
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
		private String svnDir = "";

		@Parameter(names = "-scenario", required = true)
		private String scenario = "";
	}

	private static class ScenarioName {
		private static String smartCity = "smartCity";
		private static String baseCase = "baseCase";
		private static String locationChoice = "locationChoice";
		private static String bikeHighways = "bikeHighways";
		private static String bikeHighwaysWithBridge = "bikeHighwaysWithBridge";
		private static String healthyCity = "healthyCity";

		private static boolean isValidScenarioName(String name) {
			return smartCity.equals(name)
					|| baseCase.equals(name)
					|| locationChoice.equals(name)
					|| bikeHighways.equals(name)
					|| bikeHighwaysWithBridge.equals(name)
					|| healthyCity.equals(name);
		}

		private static String getAllNames() {
			return smartCity + ", " + baseCase + ", " + locationChoice + ", " + bikeHighways + ", " + bikeHighwaysWithBridge + ", " + healthyCity;
		}
	}
}
