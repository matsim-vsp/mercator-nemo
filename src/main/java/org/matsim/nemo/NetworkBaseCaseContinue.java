package org.matsim.nemo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.checkerframework.checker.units.qual.A;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.nemo.NetworkBaseCaseContinue.newNodeAttributes;
import java.util.stream.Collectors;

public class NetworkBaseCaseContinue {

	private static final String ID_1 = "AK_Löhne_Rehme_m_Abzw._A_30_Richtung_Rehme";

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork())
				.readFile("C:/Users/Gregor/Desktop/ruhrgebiet-v1.0-network-with-RSV.xml.gz");
		Network network = scenario.getNetwork();
		final NetworkFactory fac = network.getFactory();
		List<LinkAttributes> ExsistingLinksList = fillList();
		List<newNodeAttributes> newNodes = addList();

		for (LinkAttributes attr : ExsistingLinksList) {
			Link link = network.getLinks().get(Id.createLinkId(attr.linkId));
			link.setNumberOfLanes(attr.numberOfLanes);
			link.setCapacity(attr.capacity);
		}
		addingNodesToNetwork(fac, network, newNodes);
		new NetworkWriter(network).write("C:/Users/Gregor/Desktop/testModification123.xml");
	}

	private static List<Node> addingNodesToNetwork(final NetworkFactory fac, Network network,
			List<newNodeAttributes> newNodes) {
		List<Node> nodesToConnect = new ArrayList<>();
		
		for (int index = 0; index < newNodes.size(); index++) {
				Node n0 = fac.createNode(Id.createNodeId(newNodes.get(index).NodeId),
						new Coord(newNodes.get(index).xCoord, newNodes.get(index).yCoord));
				network.addNode(n0);
				nodesToConnect.add(n0);
		}
		for (int index = 0; index < nodesToConnect.size(); index++) {
			if (index+1 < newNodes.size()) {
				Link ll = fac.createLink(Id.createLinkId(ID_1 + index), nodesToConnect.get(index), nodesToConnect.get(index+1));
				network.addLink(ll);
				Set<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				modes.add(TransportMode.ride);
				ll.setAllowedModes(modes);
				ll.setCapacity(1000);
				ll.setFreespeed(0);
			}
		}
		
		//Link ll = fac.createLink(ID_1+ nodesToConnect.lastIndexOf(nodesToConnect), nodesToConnect.lastIndexOf(nodesToConnect), )
		//nodesToConnect.lastIndexOf(nodesToConnect)
		
		
		
		return nodesToConnect;
	}

	public static List<newNodeAttributes> addList() {

		List<newNodeAttributes> NewNodeList = new ArrayList<>();
		// NW A030 AK Löhne Rehme m Abzw. A 30 Richtung Rehme N 4
		NewNodeList.add(new newNodeAttributes(ID_1 + "1", 487327.862, 5787005.019));
		NewNodeList.add(new newNodeAttributes(ID_1 + "2", 486368.257, 5787059.247));
		NewNodeList.add(new newNodeAttributes(ID_1 + "3", 484959.694, 5786557.315));
		NewNodeList.add(new newNodeAttributes(ID_1 + "4", 483670.459, 5785737.307));
		NewNodeList.add(new newNodeAttributes(ID_1 + "5", 483342.456, 5784707.741));
		return NewNodeList;
	}

	private static List<LinkAttributes> fillList() {
		List<LinkAttributes> LinkList = new ArrayList<>();

		// NW A001 AS Münster-M AK Lotte/Osnabrück E6
		LinkList.add(new LinkAttributes("454963", 3, 6000));
		LinkList.add(new LinkAttributes("300431", 3, 6000));
		LinkList.add(new LinkAttributes("300476", 3, 6000));
		LinkList.add(new LinkAttributes("136062", 3, 6000));
		LinkList.add(new LinkAttributes("136087", 3, 6000));
		LinkList.add(new LinkAttributes("136089", 3, 6000));
		LinkList.add(new LinkAttributes("136088", 3, 6000));
		LinkList.add(new LinkAttributes("300508", 3, 6000));
		LinkList.add(new LinkAttributes("299881", 3, 6000));
		LinkList.add(new LinkAttributes("47936", 3, 6000));
		LinkList.add(new LinkAttributes("47937", 3, 6000));
		LinkList.add(new LinkAttributes("302738", 3, 6000));
		LinkList.add(new LinkAttributes("302741", 3, 6000));
		LinkList.add(new LinkAttributes("507566", 3, 6000));
		LinkList.add(new LinkAttributes("83648", 3, 6000));
		LinkList.add(new LinkAttributes("83665", 3, 6000));
		LinkList.add(new LinkAttributes("168163", 3, 6000));
		LinkList.add(new LinkAttributes("168153", 3, 6000));
		LinkList.add(new LinkAttributes("169855", 3, 6000));
		LinkList.add(new LinkAttributes("169926", 3, 6000));
		LinkList.add(new LinkAttributes("47673", 3, 6000));
		LinkList.add(new LinkAttributes("168146", 3, 6000));
		LinkList.add(new LinkAttributes("361452", 3, 6000));
		LinkList.add(new LinkAttributes("168172", 3, 6000));
		LinkList.add(new LinkAttributes("302034", 3, 6000));
		LinkList.add(new LinkAttributes("226024", 3, 6000));
		LinkList.add(new LinkAttributes("130125", 3, 6000));
		LinkList.add(new LinkAttributes("226031", 3, 6000));
		LinkList.add(new LinkAttributes("180768", 3, 6000));
		LinkList.add(new LinkAttributes("170024", 3, 6000));
		LinkList.add(new LinkAttributes("169884", 3, 6000));
		LinkList.add(new LinkAttributes("226028", 3, 6000));
		LinkList.add(new LinkAttributes("274102", 3, 6000));
		LinkList.add(new LinkAttributes("170022", 3, 6000));
		LinkList.add(new LinkAttributes("262263", 3, 6000));
		LinkList.add(new LinkAttributes("274101", 3, 6000));
		LinkList.add(new LinkAttributes("262245", 3, 6000));
		LinkList.add(new LinkAttributes("262241", 3, 6000));
		LinkList.add(new LinkAttributes("169907", 3, 6000));
		LinkList.add(new LinkAttributes("169975", 3, 6000));
		LinkList.add(new LinkAttributes("215753", 3, 6000));
		LinkList.add(new LinkAttributes("215749", 3, 6000));
		LinkList.add(new LinkAttributes("169876", 3, 6000));
		LinkList.add(new LinkAttributes("169929", 3, 6000));
		LinkList.add(new LinkAttributes("169892", 3, 6000));
		LinkList.add(new LinkAttributes("169892", 3, 6000));
		LinkList.add(new LinkAttributes("302034", 3, 6000));
		LinkList.add(new LinkAttributes("264349", 3, 6000));
		LinkList.add(new LinkAttributes("169976", 3, 6000));
		LinkList.add(new LinkAttributes("226040", 3, 6000));
		LinkList.add(new LinkAttributes("59687", 3, 6000));
		LinkList.add(new LinkAttributes("265676", 3, 6000));
		LinkList.add(new LinkAttributes("170253", 3, 6000));
		LinkList.add(new LinkAttributes("169847", 3, 6000));
		LinkList.add(new LinkAttributes("289346", 3, 6000));
		LinkList.add(new LinkAttributes("169914", 3, 6000));
		LinkList.add(new LinkAttributes("183111", 3, 6000));
		LinkList.add(new LinkAttributes("183106", 3, 6000));
		LinkList.add(new LinkAttributes("498054", 3, 6000));
		LinkList.add(new LinkAttributes("498058", 3, 6000));
		LinkList.add(new LinkAttributes("262260", 3, 6000));
		LinkList.add(new LinkAttributes("262235", 3, 6000));
		LinkList.add(new LinkAttributes("262242", 3, 6000));
		LinkList.add(new LinkAttributes("350297", 3, 6000));
		LinkList.add(new LinkAttributes("130124", 3, 6000));
		LinkList.add(new LinkAttributes("226062", 3, 6000));
		LinkList.add(new LinkAttributes("226062", 3, 6000));
		LinkList.add(new LinkAttributes("350299", 3, 6000));
		LinkList.add(new LinkAttributes("226016", 3, 6000));
		LinkList.add(new LinkAttributes("466127", 3, 6000));
		LinkList.add(new LinkAttributes("253655", 3, 6000));
		LinkList.add(new LinkAttributes("253652", 3, 6000));
		LinkList.add(new LinkAttributes("467280", 3, 6000));
		LinkList.add(new LinkAttributes("226053", 3, 6000));
		LinkList.add(new LinkAttributes("169967", 3, 6000));
		LinkList.add(new LinkAttributes("226065", 3, 6000));
		LinkList.add(new LinkAttributes("226056", 3, 6000));
		LinkList.add(new LinkAttributes("169968", 3, 6000));
		LinkList.add(new LinkAttributes("226021", 3, 6000));
		LinkList.add(new LinkAttributes("183107", 3, 6000));
		LinkList.add(new LinkAttributes("170019", 3, 6000));
		LinkList.add(new LinkAttributes("188724", 3, 6000));
		LinkList.add(new LinkAttributes("188718", 3, 6000));
		LinkList.add(new LinkAttributes("460074", 3, 6000));
		LinkList.add(new LinkAttributes("460069", 3, 6000));
		LinkList.add(new LinkAttributes("460073", 3, 6000));
		LinkList.add(new LinkAttributes("460076", 3, 6000));
		LinkList.add(new LinkAttributes("86897", 3, 6000));
		LinkList.add(new LinkAttributes("86902", 3, 6000));
		LinkList.add(new LinkAttributes("86863", 3, 6000));
		LinkList.add(new LinkAttributes("86872", 3, 6000));
		LinkList.add(new LinkAttributes("226017", 3, 6000));
		LinkList.add(new LinkAttributes("170023", 3, 6000));
		LinkList.add(new LinkAttributes("170021", 3, 6000));
		LinkList.add(new LinkAttributes("169948", 3, 6000));
		LinkList.add(new LinkAttributes("169966", 3, 6000));
		LinkList.add(new LinkAttributes("462015", 3, 6000));
		LinkList.add(new LinkAttributes("462031", 3, 6000));
		LinkList.add(new LinkAttributes("462026", 3, 6000));
		LinkList.add(new LinkAttributes("462022", 3, 6000));
		LinkList.add(new LinkAttributes("226041", 3, 6000));
		LinkList.add(new LinkAttributes("134825", 3, 6000));
		//
		LinkList.add(new LinkAttributes("225966", 3, 6000));
		LinkList.add(new LinkAttributes("462030", 3, 6000));
		LinkList.add(new LinkAttributes("462018", 3, 6000));
		LinkList.add(new LinkAttributes("462029", 3, 6000));
		LinkList.add(new LinkAttributes("462021", 3, 6000));
		LinkList.add(new LinkAttributes("186856", 3, 6000));
		LinkList.add(new LinkAttributes("186860", 3, 6000));
		LinkList.add(new LinkAttributes("186855", 3, 6000));
		LinkList.add(new LinkAttributes("186857", 3, 6000));
		LinkList.add(new LinkAttributes("86928", 3, 6000));
		LinkList.add(new LinkAttributes("86929", 3, 6000));
		LinkList.add(new LinkAttributes("86875", 3, 6000));
		LinkList.add(new LinkAttributes("86888", 3, 6000));
		LinkList.add(new LinkAttributes("460066", 3, 6000));
		LinkList.add(new LinkAttributes("460072", 3, 6000));
		LinkList.add(new LinkAttributes("460075", 3, 6000));
		LinkList.add(new LinkAttributes("460067", 3, 6000));
		LinkList.add(new LinkAttributes("169874", 3, 6000));
		LinkList.add(new LinkAttributes("188729", 3, 6000));
		LinkList.add(new LinkAttributes("188719", 3, 6000));
		LinkList.add(new LinkAttributes("170006", 3, 6000));
		LinkList.add(new LinkAttributes("169986", 3, 6000));
		LinkList.add(new LinkAttributes("169993", 3, 6000));
		LinkList.add(new LinkAttributes("183105", 3, 6000));
		LinkList.add(new LinkAttributes("183110", 3, 6000));
		LinkList.add(new LinkAttributes("467283", 3, 6000));
		LinkList.add(new LinkAttributes("467286", 3, 6000));
		LinkList.add(new LinkAttributes("253653", 3, 6000));
		LinkList.add(new LinkAttributes("253654", 3, 6000));
		LinkList.add(new LinkAttributes("226052", 3, 6000));
		LinkList.add(new LinkAttributes("226055", 3, 6000));
		LinkList.add(new LinkAttributes("226059", 3, 6000));
		LinkList.add(new LinkAttributes("226018", 3, 6000));
		LinkList.add(new LinkAttributes("226061", 3, 6000));
		LinkList.add(new LinkAttributes("226038", 3, 6000));
		LinkList.add(new LinkAttributes("350289", 3, 6000));
		LinkList.add(new LinkAttributes("350292", 3, 6000));
		LinkList.add(new LinkAttributes("350298", 3, 6000));
		LinkList.add(new LinkAttributes("85776", 3, 6000));
		LinkList.add(new LinkAttributes("85777", 3, 6000));
		LinkList.add(new LinkAttributes("183108", 3, 6000));
		LinkList.add(new LinkAttributes("183219", 3, 6000));
		LinkList.add(new LinkAttributes("169845", 3, 6000));
		LinkList.add(new LinkAttributes("289354", 3, 6000));
		LinkList.add(new LinkAttributes("169904", 3, 6000));
		LinkList.add(new LinkAttributes("169904", 3, 6000));
		LinkList.add(new LinkAttributes("170005", 3, 6000));
		LinkList.add(new LinkAttributes("301472", 3, 6000));
		LinkList.add(new LinkAttributes("301462", 3, 6000));
		LinkList.add(new LinkAttributes("59691", 3, 6000));
		LinkList.add(new LinkAttributes("59693", 3, 6000));
		LinkList.add(new LinkAttributes("264363", 3, 6000));
		LinkList.add(new LinkAttributes("264358", 3, 6000));
		LinkList.add(new LinkAttributes("169934", 3, 6000));
		LinkList.add(new LinkAttributes("169915", 3, 6000));
		LinkList.add(new LinkAttributes("169920", 3, 6000));
		LinkList.add(new LinkAttributes("169945", 3, 6000));
		LinkList.add(new LinkAttributes("169945", 3, 6000));
		LinkList.add(new LinkAttributes("215747", 3, 6000));
		LinkList.add(new LinkAttributes("215750", 3, 6000));
		LinkList.add(new LinkAttributes("169869", 3, 6000));
		LinkList.add(new LinkAttributes("170001", 3, 6000));
		LinkList.add(new LinkAttributes("169946", 3, 6000));
		LinkList.add(new LinkAttributes("169977", 3, 6000));
		LinkList.add(new LinkAttributes("262238", 3, 6000));
		LinkList.add(new LinkAttributes("262267", 3, 6000));
		LinkList.add(new LinkAttributes("262270", 3, 6000));
		LinkList.add(new LinkAttributes("262259", 3, 6000));
		LinkList.add(new LinkAttributes("274097", 3, 6000));
		LinkList.add(new LinkAttributes("169950", 3, 6000));
		LinkList.add(new LinkAttributes("170010", 3, 6000));
		LinkList.add(new LinkAttributes("169881", 3, 6000));
		LinkList.add(new LinkAttributes("180759", 3, 6000));
		LinkList.add(new LinkAttributes("180767", 3, 6000));
		LinkList.add(new LinkAttributes("130110", 3, 6000));
		LinkList.add(new LinkAttributes("53713", 3, 6000));
		LinkList.add(new LinkAttributes("301913", 3, 6000));
		LinkList.add(new LinkAttributes("301912", 3, 6000));
		LinkList.add(new LinkAttributes("53702", 3, 6000));
		LinkList.add(new LinkAttributes("168170", 3, 6000));
		LinkList.add(new LinkAttributes("168169", 3, 6000));
		LinkList.add(new LinkAttributes("168148", 3, 6000));
		LinkList.add(new LinkAttributes("168145", 3, 6000));
		LinkList.add(new LinkAttributes("169922", 3, 6000));
		LinkList.add(new LinkAttributes("169875", 3, 6000));
		LinkList.add(new LinkAttributes("60537", 3, 6000));
		LinkList.add(new LinkAttributes("168156", 3, 6000));
		LinkList.add(new LinkAttributes("168168", 3, 6000));
		LinkList.add(new LinkAttributes("83657", 3, 6000));
		LinkList.add(new LinkAttributes("83662", 3, 6000));
		LinkList.add(new LinkAttributes("302737", 3, 6000));
		LinkList.add(new LinkAttributes("302734", 3, 6000));
		LinkList.add(new LinkAttributes("501808", 3, 6000));
		LinkList.add(new LinkAttributes("48647", 3, 6000));
		LinkList.add(new LinkAttributes("463679", 3, 6000));
		LinkList.add(new LinkAttributes("48654", 3, 6000));
		LinkList.add(new LinkAttributes("300520", 3, 6000));
		LinkList.add(new LinkAttributes("300513", 3, 6000));
		LinkList.add(new LinkAttributes("136090", 3, 6000));
		LinkList.add(new LinkAttributes("136084", 3, 6000));
		LinkList.add(new LinkAttributes("136091", 3, 6000));
		LinkList.add(new LinkAttributes("136083", 3, 6000));
		LinkList.add(new LinkAttributes("300494", 3, 6000));
		LinkList.add(new LinkAttributes("300493", 3, 6000));
		LinkList.add(new LinkAttributes("300437", 3, 6000));
		LinkList.add(new LinkAttributes("300436", 3, 6000));
		LinkList.add(new LinkAttributes("454982", 3, 6000));
		LinkList.add(new LinkAttributes("501901", 3, 6000));
		LinkList.add(new LinkAttributes("21295", 3, 6000));

		// NW A001 Köln / Niehl AK Leverkusen E 8
		LinkList.add(new LinkAttributes("161255", 4, 8000));
		LinkList.add(new LinkAttributes("203991", 4, 8000));
		LinkList.add(new LinkAttributes("161224", 4, 8000));
		LinkList.add(new LinkAttributes("150239", 4, 8000));
		LinkList.add(new LinkAttributes("188379", 4, 8000));
		LinkList.add(new LinkAttributes("150242", 4, 8000));
		LinkList.add(new LinkAttributes("161252", 4, 8000));
		LinkList.add(new LinkAttributes("161214", 4, 8000));
		LinkList.add(new LinkAttributes("161228", 4, 8000));
		LinkList.add(new LinkAttributes("66148", 4, 8000));
		LinkList.add(new LinkAttributes("66140", 4, 8000));
		LinkList.add(new LinkAttributes("34919", 4, 8000));
		LinkList.add(new LinkAttributes("67855", 4, 8000));
		LinkList.add(new LinkAttributes("202837", 4, 8000));
		LinkList.add(new LinkAttributes("67851", 4, 8000));
		LinkList.add(new LinkAttributes("34922", 4, 8000));
		LinkList.add(new LinkAttributes("67867", 4, 8000));
		LinkList.add(new LinkAttributes("494291", 4, 8000));
		LinkList.add(new LinkAttributes("34918", 4, 8000));
		LinkList.add(new LinkAttributes("223459", 4, 8000));
		LinkList.add(new LinkAttributes("161477", 4, 8000));
		LinkList.add(new LinkAttributes("67868", 4, 8000));
		LinkList.add(new LinkAttributes("177877", 4, 8000));
		LinkList.add(new LinkAttributes("248088", 4, 8000));
		LinkList.add(new LinkAttributes("248087", 4, 8000));
		LinkList.add(new LinkAttributes("98901", 4, 8000));
		LinkList.add(new LinkAttributes("153757", 4, 8000));
		LinkList.add(new LinkAttributes("12115", 4, 8000));
		LinkList.add(new LinkAttributes("80177", 4, 8000));
		LinkList.add(new LinkAttributes("253341", 4, 8000));
		LinkList.add(new LinkAttributes("248086", 4, 8000));
		LinkList.add(new LinkAttributes("391240", 4, 8000));
		LinkList.add(new LinkAttributes("67870", 4, 8000));
		LinkList.add(new LinkAttributes("139704", 4, 8000));
		LinkList.add(new LinkAttributes("223461", 4, 8000));
		LinkList.add(new LinkAttributes("223460", 4, 8000));
		LinkList.add(new LinkAttributes("67865", 4, 8000));
		LinkList.add(new LinkAttributes("150195", 4, 8000));
		LinkList.add(new LinkAttributes("67858", 4, 8000));
		LinkList.add(new LinkAttributes("67823", 4, 8000));
		LinkList.add(new LinkAttributes("139695", 4, 8000));
		LinkList.add(new LinkAttributes("34914", 4, 8000));
		LinkList.add(new LinkAttributes("66116", 4, 8000));
		LinkList.add(new LinkAttributes("351956", 4, 8000));
		LinkList.add(new LinkAttributes("66144", 4, 8000));
		LinkList.add(new LinkAttributes("149575", 4, 8000));
		LinkList.add(new LinkAttributes("341111", 4, 8000));
		LinkList.add(new LinkAttributes("161229", 4, 8000));
		LinkList.add(new LinkAttributes("341112", 4, 8000));
		LinkList.add(new LinkAttributes("341113", 4, 8000));
		LinkList.add(new LinkAttributes("161223", 4, 8000));
		LinkList.add(new LinkAttributes("150698", 4, 8000));
		LinkList.add(new LinkAttributes("66172", 4, 8000));
		LinkList.add(new LinkAttributes("505869", 4, 8000));
		LinkList.add(new LinkAttributes("449211", 4, 8000));

		// NW A001 AS Wermelskirchen T+R-Anlage Remscheid E6
		//

		// NW A 003 AS Köln / Mülheim AK Leverkusen (incl.) E 8
		LinkList.add(new LinkAttributes("61355", 4, 8000));
		LinkList.add(new LinkAttributes("478238", 4, 8000));
		LinkList.add(new LinkAttributes("478241", 4, 8000));
		LinkList.add(new LinkAttributes("148327", 4, 8000));
		LinkList.add(new LinkAttributes("151277", 4, 8000));
		LinkList.add(new LinkAttributes("394559", 4, 8000));
		LinkList.add(new LinkAttributes("332372", 4, 8000));
		LinkList.add(new LinkAttributes("148090", 4, 8000));
		LinkList.add(new LinkAttributes("192406", 4, 8000));
		LinkList.add(new LinkAttributes("148328", 4, 8000));
		LinkList.add(new LinkAttributes("219204", 4, 8000));
		LinkList.add(new LinkAttributes("148089", 4, 8000));
		LinkList.add(new LinkAttributes("148330", 4, 8000));
		LinkList.add(new LinkAttributes("255701", 4, 8000));
		LinkList.add(new LinkAttributes("476668", 4, 8000));
		LinkList.add(new LinkAttributes("370863", 4, 8000));
		LinkList.add(new LinkAttributes("255706", 4, 8000));
		LinkList.add(new LinkAttributes("230110", 4, 8000));
		LinkList.add(new LinkAttributes("422336", 4, 8000));
		//
		LinkList.add(new LinkAttributes("230093", 4, 8000));
		LinkList.add(new LinkAttributes("230105", 4, 8000));
		LinkList.add(new LinkAttributes("65320", 4, 8000));
		LinkList.add(new LinkAttributes("65320", 4, 8000));
		LinkList.add(new LinkAttributes("255703", 4, 8000));
		LinkList.add(new LinkAttributes("476656", 4, 8000));
		LinkList.add(new LinkAttributes("184840", 4, 8000));
		LinkList.add(new LinkAttributes("52502", 4, 8000));
		LinkList.add(new LinkAttributes("65323", 4, 8000));
		LinkList.add(new LinkAttributes("431516", 4, 8000));
		LinkList.add(new LinkAttributes("152764", 4, 8000));
		LinkList.add(new LinkAttributes("65343", 4, 8000));
		LinkList.add(new LinkAttributes("332373", 4, 8000));
		LinkList.add(new LinkAttributes("332376", 4, 8000));
		LinkList.add(new LinkAttributes("152643", 4, 8000));
		LinkList.add(new LinkAttributes("151289", 4, 8000));
		LinkList.add(new LinkAttributes("61359", 4, 8000));
		LinkList.add(new LinkAttributes("61358", 4, 8000));
		LinkList.add(new LinkAttributes("61333", 4, 8000));

		// NW A 045 AK Hagen (A 45) AK Westhofen (A 1) E 6
		LinkList.add(new LinkAttributes("494734", 3, 6000));
		LinkList.add(new LinkAttributes("245830", 3, 6000));
		LinkList.add(new LinkAttributes("245831", 3, 6000));
		LinkList.add(new LinkAttributes("356898", 3, 6000));
		LinkList.add(new LinkAttributes("74360", 3, 6000));
		LinkList.add(new LinkAttributes("45244", 3, 6000));
		LinkList.add(new LinkAttributes("435518", 3, 6000));
		LinkList.add(new LinkAttributes("435524", 3, 6000));
		LinkList.add(new LinkAttributes("48134", 3, 6000));
		LinkList.add(new LinkAttributes("266683", 3, 6000));
		LinkList.add(new LinkAttributes("145835", 3, 6000));
		LinkList.add(new LinkAttributes("353300", 3, 6000));
		LinkList.add(new LinkAttributes("353311", 3, 6000));
		LinkList.add(new LinkAttributes("353297", 3, 6000));
		LinkList.add(new LinkAttributes("256269", 3, 6000));
		LinkList.add(new LinkAttributes("353310", 3, 6000));
		LinkList.add(new LinkAttributes("349977", 3, 6000));
		LinkList.add(new LinkAttributes("349982", 3, 6000));
		LinkList.add(new LinkAttributes("73172", 3, 6000));
		LinkList.add(new LinkAttributes("73171", 3, 6000));
		LinkList.add(new LinkAttributes("73175", 3, 6000));
		LinkList.add(new LinkAttributes("201441", 3, 6000));
		LinkList.add(new LinkAttributes("86044", 3, 6000));
		LinkList.add(new LinkAttributes("86054", 3, 6000));
		LinkList.add(new LinkAttributes("85883", 3, 6000));
		LinkList.add(new LinkAttributes("86056", 3, 6000));
		LinkList.add(new LinkAttributes("35175", 3, 6000));
		LinkList.add(new LinkAttributes("63830", 3, 6000));
		LinkList.add(new LinkAttributes("63829", 3, 6000));
		//
		LinkList.add(new LinkAttributes("356581", 3, 6000));
		LinkList.add(new LinkAttributes("356574", 3, 6000));
		LinkList.add(new LinkAttributes("86039", 3, 6000));
		LinkList.add(new LinkAttributes("86046", 3, 6000));
		LinkList.add(new LinkAttributes("86047", 3, 6000));
		LinkList.add(new LinkAttributes("129462", 3, 6000));
		LinkList.add(new LinkAttributes("129474", 3, 6000));
		LinkList.add(new LinkAttributes("129465", 3, 6000));
		LinkList.add(new LinkAttributes("161769", 3, 6000));
		LinkList.add(new LinkAttributes("161784", 3, 6000));
		LinkList.add(new LinkAttributes("460598", 3, 6000));
		LinkList.add(new LinkAttributes("460599", 3, 6000));
		LinkList.add(new LinkAttributes("349989", 3, 6000));
		LinkList.add(new LinkAttributes("353298", 3, 6000));
		LinkList.add(new LinkAttributes("353296", 3, 6000));
		LinkList.add(new LinkAttributes("256230", 3, 6000));
		LinkList.add(new LinkAttributes("353299", 3, 6000));
		LinkList.add(new LinkAttributes("145830", 3, 6000));
		LinkList.add(new LinkAttributes("145834", 3, 6000));
		LinkList.add(new LinkAttributes("145840", 3, 6000));
		LinkList.add(new LinkAttributes("435521", 3, 6000));
		LinkList.add(new LinkAttributes("189992", 3, 6000));
		LinkList.add(new LinkAttributes("435525", 3, 6000));
		LinkList.add(new LinkAttributes("396642", 3, 6000));
		LinkList.add(new LinkAttributes("412071", 3, 6000));
		LinkList.add(new LinkAttributes("356913", 3, 6000));
		LinkList.add(new LinkAttributes("245918", 3, 6000));
		LinkList.add(new LinkAttributes("245919", 3, 6000));
		LinkList.add(new LinkAttributes("190768", 3, 6000));
		LinkList.add(new LinkAttributes("387646", 3, 6000));
		LinkList.add(new LinkAttributes("387651", 3, 6000));

		// NW A 046 Westring AK Sonnborn (L 418) E 6
		LinkList.add(new LinkAttributes("45323", 3, 6000));
		LinkList.add(new LinkAttributes("45341", 3, 6000));
		LinkList.add(new LinkAttributes("92013", 3, 6000));
		LinkList.add(new LinkAttributes("293605", 3, 6000));
		LinkList.add(new LinkAttributes("478154", 3, 6000));
		LinkList.add(new LinkAttributes("194808", 3, 6000));
		LinkList.add(new LinkAttributes("293602", 3, 6000));
		LinkList.add(new LinkAttributes("270096", 3, 6000));
		//
		LinkList.add(new LinkAttributes("241734", 3, 6000));
		LinkList.add(new LinkAttributes("266985", 3, 6000));
		LinkList.add(new LinkAttributes("252862", 3, 6000));
		LinkList.add(new LinkAttributes("478147", 3, 6000));
		LinkList.add(new LinkAttributes("293616", 3, 6000));
		LinkList.add(new LinkAttributes("247872", 3, 6000));
		LinkList.add(new LinkAttributes("328728", 3, 6000));
		LinkList.add(new LinkAttributes("45325", 3, 6000));

		// NW A 524 Duisburg / Serm (B 8) AS Duisburg / Rahm mit B 8 OU Düsseldorf /
		// Wittlaer (1.BA) E4
		LinkList.add(new LinkAttributes("241734", 2, 4000));
		LinkList.add(new LinkAttributes("233393", 2, 4000));
		LinkList.add(new LinkAttributes("305378", 2, 4000));
		LinkList.add(new LinkAttributes("331781", 2, 4000));
		LinkList.add(new LinkAttributes("79278", 2, 4000));
		LinkList.add(new LinkAttributes("85068", 2, 4000));
		LinkList.add(new LinkAttributes("85067", 2, 4000));
		LinkList.add(new LinkAttributes("239049", 2, 4000));
		//
		LinkList.add(new LinkAttributes("331776", 2, 4000));
		LinkList.add(new LinkAttributes("331777", 2, 4000));
		LinkList.add(new LinkAttributes("331778", 2, 4000));
		LinkList.add(new LinkAttributes("79280", 2, 4000));
		LinkList.add(new LinkAttributes("233336", 2, 4000));

		return LinkList;
	}

	private static class LinkAttributes {
		private String linkId;
		private int numberOfLanes;
		private int capacity;

		LinkAttributes(String linkId, int numberOfLanes, int capacity) {
			this.linkId = linkId;
			this.numberOfLanes = numberOfLanes;
			this.capacity = capacity;
		}
	}

	public static class newNodeAttributes {
		private String NodeId;
		private double xCoord;
		private double yCoord;

		newNodeAttributes(String NodeId, double xCoord, double yCoord) {
			this.NodeId = NodeId;
			this.xCoord = xCoord;
			this.yCoord = yCoord;
		}
	}

	@SuppressWarnings("unused")
	private static void connectNodeToNetwork(Network network, List<Node> nodesToAvoid, Node node,
			final NetworkFactory fac) {

		// search for possible connections
		Collection<Node> nodes = getNearestNodes(network, node);
		nodes.stream()
		.filter(nearNode -> !nodesToAvoid.contains(nearNode))
		.sorted((node2, node1) -> {
			Double dist1 = NetworkUtils.getEuclideanDistance(node1.getCoord(), node.getCoord());
			Double dist2 = NetworkUtils.getEuclideanDistance(node2.getCoord(), node.getCoord());
			return dist2.compareTo(dist1);
		})
				.limit(1).forEach(nearNode -> {
					Link l = fac.createLink(Id.createLinkId(ID_1 + node.getId().toString()), node, nearNode);
					Set<String> modes = new HashSet<>();
					modes.add(TransportMode.car);
					modes.add(TransportMode.ride);
					l.setAllowedModes(modes);
					l.setCapacity(1000);
					l.setFreespeed(0);
					System.out.println(l);
					network.addLink(l);
				});
	}
	

	private static Collection<Node> getNearestNodes(Network network, Node node) {

		final double distance = 300; // search nodes in a 300m radius
		return NetworkUtils.getNearestNodes(network, node.getCoord(), distance).stream()
				.filter(n -> !n.getId().toString().startsWith("pt")).collect(Collectors.toList());
	}

}
