package org.matsim.scenarioCreation.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class CreateLongDistanceBikePaths {
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		createNetworkFromCSV("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//RSV//Coord.csv//", network);
		Network emptyNetwork = NetworkUtils.createNetwork();
		NetworkFactory fac = network.getFactory();
		List<Node> nodesInOrder = new LinkedList<>();
		
		for (Node nodes : network.getNodes().values()) {
			nodesInOrder.add(nodes);
		}
		List<Link> links = new ArrayList<Link>();
		for (Node node : nodesInOrder) {
			double smallestDistance = Double.MAX_VALUE;
			Node nearestNode = null;
			for (Node nodeToCompare : nodesInOrder) {
				if (node.getCoord().getX() < nodeToCompare.getCoord().getX()) {
					double distance = CoordUtils.calcEuclideanDistance(node.getCoord(), nodeToCompare.getCoord());
					if (distance < smallestDistance && !node.equals(nodeToCompare)
							&& !containsLinksWithNodes(links, node, nodeToCompare)) {
						smallestDistance = distance;
						nearestNode = nodeToCompare;
					}
				}
			}
			if (nearestNode != null) {
				links.add(fac.createLink(Id.createLinkId(String.valueOf(Math.random())), node, nearestNode));
			}
			emptyNetwork.addNode(node);

		}
		for (Link link : links) {
			emptyNetwork.addLink(link);
		}

		// write network
		new NetworkWriter(emptyNetwork).write("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//RSV//Network_RSV_Test.xml//");
	}

	private static boolean containsLinksWithNodes(List<Link> links, Node node, Node nodeToCompare) {
		return links.stream().anyMatch(link -> isMatch(link, node, nodeToCompare));
	}

	private static boolean isMatch(Link link, Node node, Node nodeToCompare) {
		return (link.getFromNode().equals(node) && link.getToNode().equals(nodeToCompare))
				|| (link.getFromNode().equals(nodeToCompare) && link.getToNode().equals(node));
	}
	
	private static void createNetworkFromCSV (String fileName, Network net) {
		
		Scanner inputStream = null;
		try {
			File file = new File(fileName);
			inputStream = new Scanner(file);
			
			inputStream.forEachRemaining(data -> {
				String[] arr = data.split(";");
				Coord CoordOfNode = new Coord(Double.parseDouble((arr[0])), Double.parseDouble((arr[1])));
				Id<Node> NodeId = Id.createNodeId(String.valueOf(CoordOfNode.getX()));
				if (!net.getNodes().containsKey((NodeId))) {
					NetworkUtils.createAndAddNode(net, NodeId, CoordOfNode);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
}
