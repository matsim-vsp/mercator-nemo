package org.matsim.scenarioCreation.network;


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
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
/*
 * Creates the network and transit schedule+vehicles for Nemo Scenario 2
 * - fine network
 * - with car counts
 * - without bike only
 * - with bike highway
 * - cars banned from residential areas within Ruhrgebiet boundary shape
 * - links with a capacity higher than 1000 are divided into a car and bike link according to the Copenhagen model
 * - PT: number of departures is doubled by cutting the interval in half and adding a departure after every departure from OSM
 * - PT: number of vehicles doubled
 */

public class CreateFineNetworkPtAndCarCountsWithoutBikeOnlyWithBikeHighwayGesundeNachhaltigeStadt {

	private static final String SUBDIR = "fineNetworkPtAndCarCountsWithoutBikeOnlyWithBikeHighwayGesundeNachhaltigeStadt";
	private static final String FILE_PREFIX = "fineNetworkPtAndCarCountsWithoutBikeOnlyWithBikeHighwayGesundeNachhaltigeStadt";
	private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkPtAndCarCountsWithoutBikeOnlyWithBikeHighwayGesundeNachhaltigeStadt.class);

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
		addMorePtDepartures(scenarioFromOsmSchedule);

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

		// create the network from scratch
		NetworkCreator creator = new NetworkCreator.Builder().setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
				.setSvnDir(arguments.svnDir).withByciclePaths()
				.withOsmFilter(new FineNetworkFilterWithoutBikeOnlyLinks(inputParams.getInputNetworkShapeFilter()))
				.withCleaningModes(TransportMode.car, TransportMode.ride, TransportMode.bike).withRideOnCarLinks()
				.build();
		Network network = creator.createNetwork();
		
		banCarfromResidentialAreasAndCreateBikeLinks(network, inputParams.getInputNetworkShapeFilter());

		logger.info("merge transit networks and car/ride/bike network");
		MergeNetworks.merge(network, "", scenarioFromGtfsSchedule.getNetwork());

		logger.info("merge bike highways into network");
		Network highwayInput = NetworkUtils.createNetwork();
		new MatsimNetworkReader(highwayInput).readFile(inputParams.getInputBikeHighwayNetwork());
		BikeNetworkMerger merger = new BikeNetworkMerger(network);
		network = merger.mergeBikeHighways(highwayInput);

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
					for (Entry<String, Object> entry : attributes.entrySet()) {
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

	private static void addMorePtDepartures(Scenario scenario) {

		TransitSchedule schedule = scenario.getTransitSchedule();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {

				// putting all departures in a list to sort them by time

				List<Departure> departures = new ArrayList<>(route.getDepartures().values());

				departures.sort(Comparator.comparing(Departure::getDepartureTime));

				for (Departure dep : departures) {

					int index = departures.indexOf(dep);
					// add departures in between

					if (index != 0) {
						// calculate interval (not possible for first departure)
						double previousDepTime = departures.get(index - 1).getDepartureTime();
						double depTime = dep.getDepartureTime();
						double interval = depTime - previousDepTime;
						double newInterval = interval / 2.;

						if (index == 1) {
							// second departure: add new departure before and after
							Departure newDepBefore = createVehicleAndReturnDeparture(scenario,
									departures.get(index - 1), dep.getDepartureTime() - newInterval);
							Departure newDepAfter = createVehicleAndReturnDeparture(scenario, dep,
									dep.getDepartureTime() + newInterval);
							route.addDeparture(newDepAfter);
							route.addDeparture(newDepBefore);

						} else {
							// all other: add new departure after
							Departure newDepAfter = createVehicleAndReturnDeparture(scenario, dep,
									dep.getDepartureTime() + newInterval);
							route.addDeparture(newDepAfter);
						}
					}
				}
			}
		}
	}

	private static Departure createVehicleAndReturnDeparture(Scenario scenario, Departure oldDeparture, double t) {

		// create vehicle
		Id<Vehicle> oldVehicleId = oldDeparture.getVehicleId();
		Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(
				Id.createVehicleId(oldVehicleId + "_2"),
				scenario.getTransitVehicles().getVehicles().get(oldVehicleId).getType());
		scenario.getTransitVehicles().addVehicle(vehicle);

		// create departure
		Departure departure = scenario.getTransitSchedule().getFactory()
				.createDeparture(Id.create(oldDeparture.getId() + "_2", Departure.class), t);
		departure.setVehicleId(vehicle.getId());
		return departure;
	}

	private static class BikeNetworkMerger {

		private static final double MAX_LINK_LENGTH = 200;
		private static final String ID_PREFIX = "bike-highway_";
		private final Network originalNetwork;
		private final List<Link> brokenUpLinksToAdd = new ArrayList<>();
		private final List<Link> longLinksToRemove = new ArrayList<>();

		private BikeNetworkMerger(Network originalNetwork) {
			this.originalNetwork = originalNetwork;
		}

		private Network mergeBikeHighways(Network bikeHighways) {

			// break up links into parts < 200m
			this.breakLinksIntoSmallerPieces(bikeHighways);
			this.copyNodesIntoNetwork(bikeHighways);

			bikeHighways.getLinks().values().forEach(link -> {

				// copy link and give it some id
				Link newLink = copyWithUUID(originalNetwork.getFactory(), link);
				Link newReverseLink = copyWithUUIDAndReverseDirection(originalNetwork.getFactory(), link);

				connectNodeToNetwork(originalNetwork, bikeHighways.getNodes(), newLink.getFromNode());
				connectNodeToNetwork(originalNetwork, bikeHighways.getNodes(), newLink.getToNode());

				// add new links to original network
				originalNetwork.addLink(newLink);
				originalNetwork.addLink(newReverseLink);
			});

			return originalNetwork;
		}

		private void breakLinksIntoSmallerPieces(Network bikeHighways) {

			bikeHighways.getLinks().values().forEach(link -> breakUpLinkIntoSmallerPieces(bikeHighways, link));
			this.longLinksToRemove.forEach(link -> bikeHighways.removeLink(link.getId()));
			this.brokenUpLinksToAdd.forEach(bikeHighways::addLink);
		}

		private void breakUpLinkIntoSmallerPieces(Network bikeHighways, Link link) {

			double length = NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(),
					link.getToNode().getCoord());

			if (length > MAX_LINK_LENGTH) {

				longLinksToRemove.add(link);
				Node fromNode = link.getFromNode();
				Node toNode = link.getToNode();
				double numberOfParts = Math.ceil(length / 200);
				double partLength = length / numberOfParts;
				double lengthFraction = partLength / length;
				double deltaX = toNode.getCoord().getX() - fromNode.getCoord().getX();
				double deltaY = toNode.getCoord().getY() - fromNode.getCoord().getY();
				Node currentNode = fromNode;

				logger.info("link length: " + length);
				logger.info("splitting link into " + numberOfParts + " parts");

				while (numberOfParts > 1) {

					// calculate new coordinate and add a node to the network
					Coord newCoord = new Coord(currentNode.getCoord().getX() + deltaX * lengthFraction,
							currentNode.getCoord().getY() + deltaY * lengthFraction);
					Node newNode = bikeHighways.getFactory()
							.createNode(Id.createNodeId(ID_PREFIX + UUID.randomUUID().toString()), newCoord);
					bikeHighways.addNode(newNode);
					logger.info("added node with id: " + newNode.getId().toString());

					// connect current and new node with a link and add it to the network
					Link newLink = bikeHighways.getFactory().createLink(
							Id.createLinkId(ID_PREFIX + UUID.randomUUID().toString()), currentNode, newNode);
					brokenUpLinksToAdd.add(newLink);

					// wrap up for next iteration
					currentNode = newNode;
					numberOfParts--;
				}

				// last link to be inserted must be connected to currentNode and toNode
				Link lastLink = bikeHighways.getFactory()
						.createLink(Id.createLinkId(ID_PREFIX + UUID.randomUUID().toString()), currentNode, toNode);
				brokenUpLinksToAdd.add(lastLink);
			}
		}

		private void copyNodesIntoNetwork(Network fromNetwork) {
			fromNetwork.getNodes().values().forEach(originalNetwork::addNode);
		}

		private Link copyWithUUID(NetworkFactory factory, Link link) {
			return createLinkWithAttributes(factory, link.getFromNode(), link.getToNode());
		}

		private Link copyWithUUIDAndReverseDirection(NetworkFactory factory, Link link) {
			return createLinkWithAttributes(factory, link.getToNode(), link.getFromNode());
		}

		private Link createLinkWithAttributes(NetworkFactory factory, Node fromNode, Node toNode) {

			Link result = factory.createLink(Id.createLinkId("bike-highway_" + UUID.randomUUID().toString()), fromNode,
					toNode);
			result.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.bike)));
			result.setCapacity(10000); // set to pretty much unlimited
			result.setFreespeed(8.3); // 30km/h
			result.getAttributes().putAttribute(BikeLinkSpeedCalculator.BIKE_SPEED_FACTOR_KEY, 1.0); // bikes can reach
																										// their max
																										// velocity on
																										// bike highways
			result.setNumberOfLanes(1);
			result.setLength(NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
			return result;
		}

		private void connectNodeToNetwork(Network network, Map<Id<Node>, ? extends Node> nodesToAvoid, Node node) {

			// search for possible connections
			Collection<Node> nodes = getNearestNodes(network, node);
			nodes.stream().filter(nearNode -> !nodesToAvoid.containsKey(nearNode.getId())).sorted((node1, node2) -> {
				Double dist1 = NetworkUtils.getEuclideanDistance(node1.getCoord(), node.getCoord());
				Double dist2 = NetworkUtils.getEuclideanDistance(node2.getCoord(), node.getCoord());
				return dist1.compareTo(dist2);
			}).limit(1).forEach(nearNode -> {
				network.addLink(createLinkWithAttributes(network.getFactory(), node, nearNode));
				network.addLink(createLinkWithAttributes(network.getFactory(), nearNode, node));
			});
		}

		private Collection<Node> getNearestNodes(Network network, Node node) {

			final double distance = 100; // search nodes in a 100m radius
			return NetworkUtils.getNearestNodes(network, node.getCoord(), distance).stream()
					.filter(n -> !n.getId().toString().startsWith("pt")).collect(Collectors.toList());
		}
	}
}
