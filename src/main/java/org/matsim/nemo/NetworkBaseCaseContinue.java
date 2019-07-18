package org.matsim.nemo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkBaseCaseContinue {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Gregor/Desktop/testModification.gz");

		List<Attributes> LinkList = fillList();

		for (Attributes attr : LinkList) {
			Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(attr.linkId));
			link.setNumberOfLanes(attr.numberOfLanes);
			link.setCapacity(attr.capacity);
		}
		new NetworkWriter(scenario.getNetwork()).write("C:/Users/Gregor/Desktop/testModification123.xml"); ;
	}

	private static List<Attributes> fillList() {
		List<Attributes> LinkList = new ArrayList<>();
		
		//NW	A001	AS Münster-M	AK Lotte/Osnabrück	E6 
		LinkList.add(new Attributes("454963", 3, 6000));
		LinkList.add(new Attributes("300431", 3, 6000));
		LinkList.add(new Attributes("300476", 3, 6000));
		LinkList.add(new Attributes("136062", 3, 6000));
		LinkList.add(new Attributes("136087", 3, 6000));
		LinkList.add(new Attributes("136089", 3, 6000));
		LinkList.add(new Attributes("136088", 3, 6000));
		LinkList.add(new Attributes("300508", 3, 6000));
		LinkList.add(new Attributes("299881", 3, 6000));
		LinkList.add(new Attributes("47936", 3, 6000));
		LinkList.add(new Attributes("47937", 3, 6000));
		LinkList.add(new Attributes("302738", 3, 6000));
		LinkList.add(new Attributes("302741", 3, 6000));
		LinkList.add(new Attributes("507566", 3, 6000));
		LinkList.add(new Attributes("83648", 3, 6000));
		LinkList.add(new Attributes("83665", 3, 6000));
		LinkList.add(new Attributes("168163", 3, 6000));
		LinkList.add(new Attributes("168153", 3, 6000));
		LinkList.add(new Attributes("169855", 3, 6000));
		LinkList.add(new Attributes("169926", 3, 6000));
		LinkList.add(new Attributes("47673", 3, 6000));
		LinkList.add(new Attributes("168146", 3, 6000));
		LinkList.add(new Attributes("361452", 3, 6000));
		LinkList.add(new Attributes("168172", 3, 6000));
		LinkList.add(new Attributes("302034", 3, 6000));
		LinkList.add(new Attributes("226024", 3, 6000));
		LinkList.add(new Attributes("130125", 3, 6000));
		LinkList.add(new Attributes("226031", 3, 6000));
		LinkList.add(new Attributes("180768", 3, 6000));
		LinkList.add(new Attributes("170024", 3, 6000));
		LinkList.add(new Attributes("169884", 3, 6000));
		LinkList.add(new Attributes("226028", 3, 6000));
		LinkList.add(new Attributes("274102", 3, 6000));
		LinkList.add(new Attributes("170022", 3, 6000));
		LinkList.add(new Attributes("262263", 3, 6000));
		LinkList.add(new Attributes("274101", 3, 6000));
		LinkList.add(new Attributes("262245", 3, 6000));
		LinkList.add(new Attributes("262241", 3, 6000));
		LinkList.add(new Attributes("169907", 3, 6000));
		LinkList.add(new Attributes("169975", 3, 6000));
		LinkList.add(new Attributes("215753", 3, 6000));
		LinkList.add(new Attributes("215749", 3, 6000));
		LinkList.add(new Attributes("169876", 3, 6000));
		LinkList.add(new Attributes("169929", 3, 6000));
		LinkList.add(new Attributes("169892", 3, 6000));
		LinkList.add(new Attributes("169892", 3, 6000));
		LinkList.add(new Attributes("302034", 3, 6000));
		LinkList.add(new Attributes("264349", 3, 6000));
		LinkList.add(new Attributes("169976", 3, 6000));
		LinkList.add(new Attributes("226040", 3, 6000));
		LinkList.add(new Attributes("59687", 3, 6000));
		LinkList.add(new Attributes("265676", 3, 6000));
		LinkList.add(new Attributes("170253", 3, 6000));
		LinkList.add(new Attributes("169847", 3, 6000));
		LinkList.add(new Attributes("289346", 3, 6000));
		LinkList.add(new Attributes("169914", 3, 6000));
		LinkList.add(new Attributes("183111", 3, 6000));
		LinkList.add(new Attributes("183106", 3, 6000));
		LinkList.add(new Attributes("498054", 3, 6000));
		LinkList.add(new Attributes("498058", 3, 6000));
		LinkList.add(new Attributes("262260", 3, 6000));
		LinkList.add(new Attributes("262235", 3, 6000));
		LinkList.add(new Attributes("262242", 3, 6000));
		LinkList.add(new Attributes("350297", 3, 6000));
		LinkList.add(new Attributes("130124", 3, 6000));
		LinkList.add(new Attributes("226062", 3, 6000));
		LinkList.add(new Attributes("226062", 3, 6000));
		LinkList.add(new Attributes("350299", 3, 6000));
		LinkList.add(new Attributes("226016", 3, 6000));
		LinkList.add(new Attributes("466127", 3, 6000));
		LinkList.add(new Attributes("253655", 3, 6000));
		LinkList.add(new Attributes("253652", 3, 6000));
		LinkList.add(new Attributes("467280", 3, 6000));
		LinkList.add(new Attributes("226053", 3, 6000));
		LinkList.add(new Attributes("169967", 3, 6000));
		LinkList.add(new Attributes("226065", 3, 6000));
		LinkList.add(new Attributes("226056", 3, 6000));
		LinkList.add(new Attributes("169968", 3, 6000));
		LinkList.add(new Attributes("226021", 3, 6000));
		LinkList.add(new Attributes("183107", 3, 6000));
		LinkList.add(new Attributes("170019", 3, 6000));
		LinkList.add(new Attributes("188724", 3, 6000));
		LinkList.add(new Attributes("188718", 3, 6000));
		LinkList.add(new Attributes("460074", 3, 6000));
		LinkList.add(new Attributes("460069", 3, 6000));
		LinkList.add(new Attributes("460073", 3, 6000));
		LinkList.add(new Attributes("460076", 3, 6000));
		LinkList.add(new Attributes("86897", 3, 6000));
		LinkList.add(new Attributes("86902", 3, 6000));
		LinkList.add(new Attributes("86863", 3, 6000));
		LinkList.add(new Attributes("86872", 3, 6000));
		LinkList.add(new Attributes("226017", 3, 6000));
		LinkList.add(new Attributes("170023", 3, 6000));
		LinkList.add(new Attributes("170021", 3, 6000));
		LinkList.add(new Attributes("169948", 3, 6000));
		LinkList.add(new Attributes("169966", 3, 6000));
		LinkList.add(new Attributes("462015", 3, 6000));
		LinkList.add(new Attributes("462031", 3, 6000));
		LinkList.add(new Attributes("462026", 3, 6000));
		LinkList.add(new Attributes("462022", 3, 6000));
		LinkList.add(new Attributes("226041", 3, 6000));
		LinkList.add(new Attributes("134825", 3, 6000));
		return LinkList;
	}

	private static class Attributes {
		private String linkId;
		private int numberOfLanes;
		private int capacity;
		Attributes(String linkId, int numberOfLanes, int capacity) {
			this.linkId = linkId;
			this.numberOfLanes = numberOfLanes;
			this.capacity = capacity;
		}
		
	}
	
}
