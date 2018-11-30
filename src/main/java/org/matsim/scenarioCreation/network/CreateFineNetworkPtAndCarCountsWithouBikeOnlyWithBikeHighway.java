package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighway {

    private static final String SUBDIR = "fine_with-pt_without-bike-only-links_with-bike-highway";
    private static final String FILE_PREFIX = "nemo_fine_network_with-pt_without-bike-only-links_with-bike-highway";
    private static final String inputFile = "C:\\Users\\Janek\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\network\\fine_with-pt_without-bike-only-links\\nemo_fine_network_with-pt_without-bike-only-links.xml.gz";
    private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighway.class);

    public static void main(String[] args) throws IOException {


        // parse input
        CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighway.InputArguments arguments = new CreateFineNetworkPtAndCarCountsWithouBikeOnlyWithBikeHighway.InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        NetworkOutput outputParams = new NetworkOutput(arguments.svnDir);
        NetworkInput inputParams = new NetworkInput(arguments.svnDir);

        //ensure all output directories are present
        Files.createDirectories(outputParams.getOutputNetworkDir().resolve(SUBDIR));


        // TODO replace network from disk with network reader and stuff
        Network originalNetwork = NetworkUtils.createNetwork();
        Path inputPath = Paths.get(inputFile);
        new MatsimNetworkReader(originalNetwork).readFile(inputPath.toString());
        prepareBikeHighways(inputParams, originalNetwork);

        final Path outputNetwork = outputParams.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + ".xml.gz");
        new NetworkWriter(originalNetwork).write(outputNetwork.toString());
    }

    private static void prepareBikeHighways(NetworkInput inputParams, Network originalNetwork) {
        Network highwayInput = NetworkUtils.createNetwork();
        new MatsimNetworkReader(highwayInput).readFile(inputParams.getInputBikeHighwayNetwork());
        copyNodesIntoNetwork(highwayInput, originalNetwork);
        NetworkFactory factory = originalNetwork.getFactory();

        highwayInput.getLinks().values().forEach(link -> {

            // copy link and give it some id
            Link newLink = copyWithUUID(factory, link);
            Link newReverseLink = copyWithUUIDAndReverseDirection(factory, link);

            connectNodeToNetwork(originalNetwork, highwayInput, newLink.getFromNode());
            connectNodeToNetwork(originalNetwork, highwayInput, newLink.getToNode());

            // add new links to original network
            originalNetwork.addLink(newLink);
            originalNetwork.addLink(newReverseLink);
        });
    }

    private static void connectNodeToNetwork(Network network, Network highwayNetwork, Node node) {

        // search for possible connections with increasing radius
        Collection<Node> nodes = getNearestNodes(network, node, 30);

        if (nodes.size() == 0) {
            nodes = getNearestNodes(network, node, 50);
        }
        if (nodes.size() == 0) {
            nodes = getNearestNodes(network, node, 100);
        }
        nodes.forEach(nearNode -> {
            if (!highwayNetwork.getNodes().values().contains(nearNode)) {
                network.addLink(createLinkWithAttributes(network.getFactory(), node, nearNode));
                network.addLink(createLinkWithAttributes(network.getFactory(), nearNode, node));
            }
        });
    }

    private static Collection<Node> getNearestNodes(Network network, Node node, double distance) {
        return NetworkUtils.getNearestNodes(network, node.getCoord(), distance).stream()
                .filter(n -> !n.getId().toString().startsWith("pt")).collect(Collectors.toList());
    }

    private static Link copyWithUUID(NetworkFactory factory, Link link) {
        return createLinkWithAttributes(factory, link.getFromNode(), link.getToNode());
    }

    private static Link copyWithUUIDAndReverseDirection(NetworkFactory factory, Link link) {
        return createLinkWithAttributes(factory, link.getToNode(), link.getFromNode());
    }

    private static Link createLinkWithAttributes(NetworkFactory factory, Node fromNode, Node toNode) {

        Link result = factory.createLink(
                Id.createLinkId("bike-highway_" + UUID.randomUUID().toString()),
                fromNode, toNode
        );
        result.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.bike)));
        result.setCapacity(10000); // TODO find source for capacity
        result.setFreespeed(8.33); // 30 km/h
        result.setNumberOfLanes(1);
        result.setLength(NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
        return result;
    }

    private static void copyNodesIntoNetwork(Network fromNetwork, Network toNetwork) {
        fromNetwork.getNodes().values().forEach(toNetwork::addNode);
    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true,
                description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
        private String svnDir;
    }
}
