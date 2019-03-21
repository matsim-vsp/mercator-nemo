package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighwayAndBridge {

    private static final String SUBDIR = "fine_with-pt_without-bike-only-links_with-bike-highway_and-bridge";
    private static final String FILE_PREFIX = "nemo_fine_network_with-pt_without-bike-only-links_with-bike-highway_and-bridge";
    private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighwayAndBridge.class);

    public static void main(String[] args) throws IOException {

        // parse input
        CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighwayAndBridge.InputArguments arguments = new CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighwayAndBridge.InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        NetworkOutput outputParams = new NetworkOutput(arguments.svnDir);
        NetworkInput inputParams = new NetworkInput(arguments.svnDir);
        PtInput ptInputParams = new PtInput(arguments.svnDir);
        PtOutput ptOutputParams = new PtOutput(arguments.svnDir);

        //ensure all output directories are present
        Files.createDirectories(outputParams.getOutputNetworkDir().resolve(SUBDIR));

        // read in transit-network, vehicles and schedule from osm
        Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile().run(ptInputParams.getOsmScheduleFile().toString());

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
        NetworkCreator creator = new NetworkCreator.Builder()
                .setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
                .setSvnDir(arguments.svnDir)
                .withByciclePaths()
                .withOsmFilter(new FineNetworkFilterWithoutBikeOnlyLinks(inputParams.getInputNetworkShapeFilter()))
                .withCleaningModes(TransportMode.car, TransportMode.ride, TransportMode.bike)
                .withRideOnCarLinks()
                .build();
        Network network = creator.createNetwork();

        logger.info("merge transit networks and car/ride/bike network");
        MergeNetworks.merge(network, "", scenarioFromGtfsSchedule.getNetwork());

        logger.info("merge bike highways into network");
        Network highwayInput = NetworkUtils.createNetwork();
        new MatsimNetworkReader(highwayInput).readFile(inputParams.getInputBikeHighwayNetworkWithBridge());
        BikeNetworkMerger merger = new BikeNetworkMerger(network);
        network = merger.mergeBikeHighways(highwayInput);


        final Path outputNetwork = outputParams.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + ".xml.gz");
        logger.info("writing network to: " + outputNetwork.toString());
        new NetworkWriter(network).write(outputNetwork.toString());

        // create long term counts
        Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
        NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 50025L)
                .useCountsWithinGeometry(inputParams.getInputNetworkShapeFilter())
                .build();
        Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

        // create short term counts
        NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .useCountsWithinGeometry(inputParams.getInputNetworkShapeFilter())
                .withStationIdsToOmit(5002L, 5025L)
                .build();
        Map<String, Counts<Link>> shortTermCounts = shortTermCountsCreator.run();

        CombinedCountsWriter.writeCounts(outputParams.getOutputNetworkDir().resolve(SUBDIR), FILE_PREFIX, columnCombinations,
                longTermCounts, shortTermCounts);
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

        @Parameter(names = "-svnDir", required = true,
                description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
        private String svnDir;
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

            double length = NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());

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
                    Coord newCoord = new Coord(
                            currentNode.getCoord().getX() + deltaX * lengthFraction,
                            currentNode.getCoord().getY() + deltaY * lengthFraction
                    );
                    Node newNode = bikeHighways.getFactory().createNode(
                            Id.createNodeId(ID_PREFIX + UUID.randomUUID().toString()), newCoord
                    );
                    bikeHighways.addNode(newNode);
                    logger.info("added node with id: " + newNode.getId().toString());

                    // connect current and new node with a link and add it to the network
                    Link newLink = bikeHighways.getFactory().createLink(
                            Id.createLinkId(ID_PREFIX + UUID.randomUUID().toString()),
                            currentNode, newNode
                    );
                    brokenUpLinksToAdd.add(newLink);

                    // wrap up for next iteration
                    currentNode = newNode;
                    numberOfParts--;
                }

                // last link to be inserted must be connected to currentNode and toNode
                Link lastLink = bikeHighways.getFactory().createLink(
                        Id.createLinkId(ID_PREFIX + UUID.randomUUID().toString()),
                        currentNode, toNode
                );
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

            Link result = factory.createLink(
                    Id.createLinkId("bike-highway_" + UUID.randomUUID().toString()),
                    fromNode, toNode
            );
            result.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.bike)));
            result.setCapacity(10000); // set to pretty much unlimited
            result.setFreespeed(8.3); // 30km/h
            result.getAttributes().putAttribute(BikeLinkSpeedCalculator.BIKE_SPEED_FACTOR_KEY, 1.0); // bikes can reach their max velocity on bike highways
            result.setNumberOfLanes(1);
            result.setLength(NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
            return result;
        }

        private void connectNodeToNetwork(Network network, Map<Id<Node>, ? extends Node> nodesToAvoid, Node node) {

            // search for possible connections
            Collection<Node> nodes = getNearestNodes(network, node);
            nodes.stream()
                    .filter(nearNode -> !nodesToAvoid.containsKey(nearNode.getId()))
                    .sorted((node1, node2) -> {
                        Double dist1 = NetworkUtils.getEuclideanDistance(node1.getCoord(), node.getCoord());
                        Double dist2 = NetworkUtils.getEuclideanDistance(node2.getCoord(), node.getCoord());
                        return dist1.compareTo(dist2);
                    })
                    .limit(1)
                    .forEach(nearNode -> {
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
