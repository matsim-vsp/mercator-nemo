package org.matsim.scenarioCreation.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class FilterBikeLinksFromNetwork {
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C://Users//Gregor//Documents//VSP_Arbeit//pt//OSM_GTFS_merged_final//detailedRuhr_Network_10072018filtered_network_GTFS_OSM.xml.gz");
		Set<Link> linkwithoutcar = new HashSet<>();
		
		for (Link l: network.getLinks().values() ) {
			if (l.getAllowedModes().contains("car") || l.getAllowedModes().contains("pt") || l.getAllowedModes().contains("ride")) {
				// keep the link
			} else {
				linkwithoutcar.add(l);
			}
		}
		
		for (Link l: linkwithoutcar) {
			network.removeLink(l.getId());
		}
		new NetworkWriter(network).write("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//InputNemoTest//network_only_Pt_and_car.xml");
	}
}