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

import networkProcessing.NetworkBaseCaseContinue.Attributes;

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
		new NetworkWriter(scenario.getNetwork()).write("C:/Users/Gregor/Desktop/testModification123.xml");
		;
	}

	private static List<Attributes> fillList() {
		List<Attributes> LinkList = new ArrayList<>();

		// NW A001 AS Münster-M AK Lotte/Osnabrück E6
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
		//
		LinkList.add(new Attributes("225966", 3, 6000));
		LinkList.add(new Attributes("462030", 3, 6000));
		LinkList.add(new Attributes("462018", 3, 6000));
		LinkList.add(new Attributes("462029", 3, 6000));
		LinkList.add(new Attributes("462021", 3, 6000));
		LinkList.add(new Attributes("186856", 3, 6000));
		LinkList.add(new Attributes("186860", 3, 6000));
		LinkList.add(new Attributes("186855", 3, 6000));
		LinkList.add(new Attributes("186857", 3, 6000));
		LinkList.add(new Attributes("86928", 3, 6000));
		LinkList.add(new Attributes("86929", 3, 6000));
		LinkList.add(new Attributes("86875", 3, 6000));
		LinkList.add(new Attributes("86888", 3, 6000));
		LinkList.add(new Attributes("460066", 3, 6000));
		LinkList.add(new Attributes("460072", 3, 6000));
		LinkList.add(new Attributes("460075", 3, 6000));
		LinkList.add(new Attributes("460067", 3, 6000));
		LinkList.add(new Attributes("169874", 3, 6000));
		LinkList.add(new Attributes("188729", 3, 6000));
		LinkList.add(new Attributes("188719", 3, 6000));
		LinkList.add(new Attributes("170006", 3, 6000));
		LinkList.add(new Attributes("169986", 3, 6000));
		LinkList.add(new Attributes("169993", 3, 6000));
		LinkList.add(new Attributes("183105", 3, 6000));
		LinkList.add(new Attributes("183110", 3, 6000));
		LinkList.add(new Attributes("467283", 3, 6000));
		LinkList.add(new Attributes("467286", 3, 6000));
		LinkList.add(new Attributes("253653", 3, 6000));
		LinkList.add(new Attributes("253654", 3, 6000));
		LinkList.add(new Attributes("226052", 3, 6000));
		LinkList.add(new Attributes("226055", 3, 6000));
		LinkList.add(new Attributes("226059", 3, 6000));
		LinkList.add(new Attributes("226018", 3, 6000));
		LinkList.add(new Attributes("226061", 3, 6000));
		LinkList.add(new Attributes("226038", 3, 6000));
		LinkList.add(new Attributes("350289", 3, 6000));
		LinkList.add(new Attributes("350292", 3, 6000));
		LinkList.add(new Attributes("350298", 3, 6000));
		LinkList.add(new Attributes("85776", 3, 6000));
		LinkList.add(new Attributes("85777", 3, 6000));
		LinkList.add(new Attributes("183108", 3, 6000));
		LinkList.add(new Attributes("183219", 3, 6000));
		LinkList.add(new Attributes("169845", 3, 6000));
		LinkList.add(new Attributes("289354", 3, 6000));
		LinkList.add(new Attributes("169904", 3, 6000));
		LinkList.add(new Attributes("169904", 3, 6000));
		LinkList.add(new Attributes("170005", 3, 6000));
		LinkList.add(new Attributes("301472", 3, 6000));
		LinkList.add(new Attributes("301462", 3, 6000));
		LinkList.add(new Attributes("59691", 3, 6000));
		LinkList.add(new Attributes("59693", 3, 6000));
		LinkList.add(new Attributes("264363", 3, 6000));
		LinkList.add(new Attributes("264358", 3, 6000));
		LinkList.add(new Attributes("169934", 3, 6000));
		LinkList.add(new Attributes("169915", 3, 6000));
		LinkList.add(new Attributes("169920", 3, 6000));
		LinkList.add(new Attributes("169945", 3, 6000));
		LinkList.add(new Attributes("169945", 3, 6000));
		LinkList.add(new Attributes("215747", 3, 6000));
		LinkList.add(new Attributes("215750", 3, 6000));
		LinkList.add(new Attributes("169869", 3, 6000));
		LinkList.add(new Attributes("170001", 3, 6000));
		LinkList.add(new Attributes("169946", 3, 6000));
		LinkList.add(new Attributes("169977", 3, 6000));
		LinkList.add(new Attributes("262238", 3, 6000));
		LinkList.add(new Attributes("262267", 3, 6000));
		LinkList.add(new Attributes("262270", 3, 6000));
		LinkList.add(new Attributes("262259", 3, 6000));
		LinkList.add(new Attributes("274097", 3, 6000));
		LinkList.add(new Attributes("169950", 3, 6000));
		LinkList.add(new Attributes("170010", 3, 6000));
		LinkList.add(new Attributes("169881", 3, 6000));
		LinkList.add(new Attributes("180759", 3, 6000));
		LinkList.add(new Attributes("180767", 3, 6000));
		LinkList.add(new Attributes("130110", 3, 6000));
		LinkList.add(new Attributes("53713", 3, 6000));
		LinkList.add(new Attributes("301913", 3, 6000));
		LinkList.add(new Attributes("301912", 3, 6000));
		LinkList.add(new Attributes("53702", 3, 6000));
		LinkList.add(new Attributes("168170", 3, 6000));
		LinkList.add(new Attributes("168169", 3, 6000));
		LinkList.add(new Attributes("168148", 3, 6000));
		LinkList.add(new Attributes("168145", 3, 6000));
		LinkList.add(new Attributes("169922", 3, 6000));
		LinkList.add(new Attributes("169875", 3, 6000));
		LinkList.add(new Attributes("60537", 3, 6000));
		LinkList.add(new Attributes("168156", 3, 6000));
		LinkList.add(new Attributes("168168", 3, 6000));
		LinkList.add(new Attributes("83657", 3, 6000));
		LinkList.add(new Attributes("83662", 3, 6000));
		LinkList.add(new Attributes("302737", 3, 6000));
		LinkList.add(new Attributes("302734", 3, 6000));
		LinkList.add(new Attributes("501808", 3, 6000));
		LinkList.add(new Attributes("48647", 3, 6000));
		LinkList.add(new Attributes("463679", 3, 6000));
		LinkList.add(new Attributes("48654", 3, 6000));
		LinkList.add(new Attributes("300520", 3, 6000));
		LinkList.add(new Attributes("300513", 3, 6000));
		LinkList.add(new Attributes("136090", 3, 6000));
		LinkList.add(new Attributes("136084", 3, 6000));
		LinkList.add(new Attributes("136091", 3, 6000));
		LinkList.add(new Attributes("136083", 3, 6000));
		LinkList.add(new Attributes("300494", 3, 6000));
		LinkList.add(new Attributes("300493", 3, 6000));
		LinkList.add(new Attributes("300437", 3, 6000));
		LinkList.add(new Attributes("300436", 3, 6000));
		LinkList.add(new Attributes("454982", 3, 6000));
		LinkList.add(new Attributes("501901", 3, 6000));
		LinkList.add(new Attributes("21295", 3, 6000));

		// NW A001 Köln / Niehl AK Leverkusen E 8
		LinkList.add(new Attributes("161255", 4, 8000));
		LinkList.add(new Attributes("203991", 4, 8000));
		LinkList.add(new Attributes("161224", 4, 8000));
		LinkList.add(new Attributes("150239", 4, 8000));
		LinkList.add(new Attributes("188379", 4, 8000));
		LinkList.add(new Attributes("150242", 4, 8000));
		LinkList.add(new Attributes("161252", 4, 8000));
		LinkList.add(new Attributes("161214", 4, 8000));
		LinkList.add(new Attributes("161228", 4, 8000));
		LinkList.add(new Attributes("66148", 4, 8000));
		LinkList.add(new Attributes("66140", 4, 8000));
		LinkList.add(new Attributes("34919", 4, 8000));
		LinkList.add(new Attributes("67855", 4, 8000));
		LinkList.add(new Attributes("202837", 4, 8000));
		LinkList.add(new Attributes("67851", 4, 8000));
		LinkList.add(new Attributes("34922", 4, 8000));
		LinkList.add(new Attributes("67867", 4, 8000));
		LinkList.add(new Attributes("494291", 4, 8000));
		LinkList.add(new Attributes("34918", 4, 8000));
		LinkList.add(new Attributes("223459", 4, 8000));
		LinkList.add(new Attributes("161477", 4, 8000));
		LinkList.add(new Attributes("67868", 4, 8000));
		LinkList.add(new Attributes("177877", 4, 8000));
		LinkList.add(new Attributes("248088", 4, 8000));
		LinkList.add(new Attributes("248087", 4, 8000));
		LinkList.add(new Attributes("98901", 4, 8000));
		LinkList.add(new Attributes("153757", 4, 8000));
		LinkList.add(new Attributes("12115", 4, 8000));
		LinkList.add(new Attributes("80177", 4, 8000));
		LinkList.add(new Attributes("253341", 4, 8000));
		LinkList.add(new Attributes("248086", 4, 8000));
		LinkList.add(new Attributes("391240", 4, 8000));
		LinkList.add(new Attributes("67870", 4, 8000));
		LinkList.add(new Attributes("139704", 4, 8000));
		LinkList.add(new Attributes("223461", 4, 8000));
		LinkList.add(new Attributes("223460", 4, 8000));
		LinkList.add(new Attributes("67865", 4, 8000));
		LinkList.add(new Attributes("150195", 4, 8000));
		LinkList.add(new Attributes("67858", 4, 8000));
		LinkList.add(new Attributes("67823", 4, 8000));
		LinkList.add(new Attributes("139695", 4, 8000));
		LinkList.add(new Attributes("34914", 4, 8000));
		LinkList.add(new Attributes("66116", 4, 8000));
		LinkList.add(new Attributes("351956", 4, 8000));
		LinkList.add(new Attributes("66144", 4, 8000));
		LinkList.add(new Attributes("149575", 4, 8000));
		LinkList.add(new Attributes("341111", 4, 8000));
		LinkList.add(new Attributes("161229", 4, 8000));
		LinkList.add(new Attributes("341112", 4, 8000));
		LinkList.add(new Attributes("341113", 4, 8000));
		LinkList.add(new Attributes("161223", 4, 8000));
		LinkList.add(new Attributes("150698", 4, 8000));
		LinkList.add(new Attributes("66172", 4, 8000));
		LinkList.add(new Attributes("505869", 4, 8000));
		LinkList.add(new Attributes("449211", 4, 8000));

		// NW A001 AS Wermelskirchen T+R-Anlage Remscheid E6
		//

		// NW A 003 AS Köln / Mülheim AK Leverkusen (incl.) E 8
		LinkList.add(new Attributes("61355", 4, 8000));
		LinkList.add(new Attributes("478238", 4, 8000));
		LinkList.add(new Attributes("478241", 4, 8000));
		LinkList.add(new Attributes("148327", 4, 8000));
		LinkList.add(new Attributes("151277", 4, 8000));
		LinkList.add(new Attributes("394559", 4, 8000));
		LinkList.add(new Attributes("332372", 4, 8000));
		LinkList.add(new Attributes("148090", 4, 8000));
		LinkList.add(new Attributes("192406", 4, 8000));
		LinkList.add(new Attributes("148328", 4, 8000));
		LinkList.add(new Attributes("219204", 4, 8000));
		LinkList.add(new Attributes("148089", 4, 8000));
		LinkList.add(new Attributes("148330", 4, 8000));
		LinkList.add(new Attributes("255701", 4, 8000));
		LinkList.add(new Attributes("476668", 4, 8000));
		LinkList.add(new Attributes("370863", 4, 8000));
		LinkList.add(new Attributes("255706", 4, 8000));
		LinkList.add(new Attributes("230110", 4, 8000));
		LinkList.add(new Attributes("422336", 4, 8000));
		//

		LinkList.add(new Attributes("230093", 4, 8000));
		LinkList.add(new Attributes("230105", 4, 8000));
		LinkList.add(new Attributes("65320", 4, 8000));
		LinkList.add(new Attributes("65320", 4, 8000));
		LinkList.add(new Attributes("255703", 4, 8000));
		LinkList.add(new Attributes("476656", 4, 8000));
		LinkList.add(new Attributes("184840", 4, 8000));
		LinkList.add(new Attributes("52502", 4, 8000));
		LinkList.add(new Attributes("65323", 4, 8000));
		LinkList.add(new Attributes("431516", 4, 8000));
		LinkList.add(new Attributes("152764", 4, 8000));
		LinkList.add(new Attributes("65343", 4, 8000));
		LinkList.add(new Attributes("332373", 4, 8000));
		LinkList.add(new Attributes("332376", 4, 8000));
		LinkList.add(new Attributes("152643", 4, 8000));
		LinkList.add(new Attributes("151289", 4, 8000));
		LinkList.add(new Attributes("61359", 4, 8000));
		LinkList.add(new Attributes("61358", 4, 8000));
		LinkList.add(new Attributes("61333", 4, 8000));

		// NW A 045 AK Hagen (A 45) AK Westhofen (A 1) E 6
		LinkList.add(new Attributes("494734", 3, 6000));
		LinkList.add(new Attributes("245830", 3, 6000));
		LinkList.add(new Attributes("245831", 3, 6000));
		LinkList.add(new Attributes("356898", 3, 6000));
		LinkList.add(new Attributes("74360", 3, 6000));
		LinkList.add(new Attributes("45244", 3, 6000));
		LinkList.add(new Attributes("435518", 3, 6000));
		LinkList.add(new Attributes("435524", 3, 6000));
		LinkList.add(new Attributes("48134", 3, 6000));
		LinkList.add(new Attributes("266683", 3, 6000));
		LinkList.add(new Attributes("145835", 3, 6000));
		LinkList.add(new Attributes("353300", 3, 6000));

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
