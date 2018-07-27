/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.scenarioCreation.pt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;

/**
* @author ikaddoura
*/

public class AddDeparturesToTransitNetworkFromOSM {
	
	private final double trainBeelineDistanceFactor = 1.3;
	
	private Scenario scenario;
	private static final Logger log = Logger.getLogger(GeneratePtAndRunNEMO.class);
	private int vehicleCounter = 0;
	private int lineCounter = 0;

	public static void main(String[] args) {
		
		AddDeparturesToTransitNetworkFromOSM runner = new AddDeparturesToTransitNetworkFromOSM();
		runner.run();
	}

	public void run() {
		
		// adjust these directories
		final String projectDirectory = "D:/Arbeit/mercator-nemo/";
//		final String projectDirectory = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/";
		
		final String inputScheduleFile = projectDirectory + "data/pt/ptNetworkScheduleFileFromOSM.xml"; 
		final String directory = projectDirectory + "data/pt/extendedScheduleFromOSM/";
		
		final String outputScheduleFile = directory + "OSMtransitSchedule.xml.gz";
		final String outptTransitVehicleFile = directory + "OSMtransitVehicles.xml.gz";
		final String outputNetworkFile = directory + "OSMtransitNetwork.xml.gz";
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(directory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("input transit schedule file: " + inputScheduleFile);
		log.info("output schedule file: " + outputScheduleFile);
		log.info("output vehicles file: " + outptTransitVehicleFile);
				
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(directory + "/osm-pt-visualization/");
		config.controler().setRunId("osm-pt-v1");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		
//		List<String> networkModes = new ArrayList<>();
//		networkModes.add("train");
//		config.plansCalcRoute().setNetworkModes(networkModes);
		
		config.qsim().setEndTime(30. * 3600.);
		config.qsim().setStartTime(0.);
		
		config.plans().setInputFile(null);
		config.transit().setUseTransit(true);
		
		Scenario scenario0 = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario0).readFile(inputScheduleFile);
		TransitSchedule schedule0 = scenario0.getTransitSchedule();

		scenario = ScenarioUtils.createScenario(config);
		
		Map<Id<TransitStopFacility>, TransitStopFacility> transitStopFacilities = new HashMap<>();
		
		final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", NEMOUtils.NEMO_EPSG);
		
		for (TransitStopFacility transitStopFacility : scenario0.getTransitSchedule().getFacilities().values()) {
			TransitStopFacility transitStopFacilityNew = scenario.getTransitSchedule().getFactory().createTransitStopFacility(transitStopFacility.getId(), ct.transform(transitStopFacility.getCoord()), transitStopFacility.getIsBlockingLane());
			transitStopFacilityNew.setName(transitStopFacility.getName());
			transitStopFacilities.put(transitStopFacilityNew.getId(), transitStopFacilityNew);
		}
		
		// create transit vehicle type
		VehicleType type = scenario.getTransitVehicles().getFactory().createVehicleType(Id.create("default-train", VehicleType.class));
		type.setLength(100.);
		type.setEgressTime(0.);
		type.setAccessTime(0.);
		VehicleCapacity capacity = scenario.getTransitVehicles().getFactory().createVehicleCapacity();
		capacity.setSeats(500);
		type.setCapacity(capacity);
		scenario.getTransitVehicles().addVehicleType(type);
		
		// create transit lines
		for (TransitLine line : schedule0.getTransitLines().values()) {
			log.info("-------------");
			log.info("line: " + line.getId());
			
			Id<TransitLine> lineId = Id.create("line"  + lineCounter, TransitLine.class);
			TransitLine lineNew = scenario.getTransitSchedule().getFactory().createTransitLine(lineId);
			lineNew.setName(line.getId().toString().replaceAll("[^a-zA-Z0-9]", "-"));
		
			int routeCounter = 0;
			for (TransitRoute route : line.getRoutes().values()) {
				log.info("		> route: " + route.getId());
				
				Id<TransitRoute> routeNewId = Id.create("route" + routeCounter, TransitRoute.class);

				List<Id<Link>> linkIds = new ArrayList<>();
				List<TransitRouteStop> stops = new ArrayList<>();
				double departureOffset = 0.;

				// generate network links
				TransitStopFacility previousStopFacility = null;
				int transitStopCounter = 0;
				for (TransitRouteStop transitRouteStop : route.getStops()) {
					
					TransitStopFacility stopFacilityFromOSM = transitStopFacilities.get(transitRouteStop.getStopFacility().getId());
					
					TransitStopFacility transitStopFacilityNew = scenario.getTransitSchedule().getFactory().createTransitStopFacility(Id.create(lineCounter + "_" + routeCounter + "_" + stopFacilityFromOSM.getId().toString() + "_" + transitStopCounter, TransitStopFacility.class), stopFacilityFromOSM.getCoord(), stopFacilityFromOSM.getIsBlockingLane());
					transitStopFacilityNew.setName(stopFacilityFromOSM.getName());
					
					log.info("Adding stop facility " + transitStopFacilityNew.getId());
					scenario.getTransitSchedule().addStopFacility(transitStopFacilityNew);
					
					Link link;
					if (previousStopFacility == null) {
						// create a 'help' link upstream of first stop
						
						Node toNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId(lineId + "_" + routeNewId + "_" + transitStopFacilityNew.getId() + "_" + transitStopCounter), transitStopFacilityNew.getCoord());
						scenario.getNetwork().addNode(toNode);

						// add a new node
						Node fromNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId("start_" + lineId + "_" + routeNewId + "_" + transitStopFacilityNew.getId()), new Coord(toNode.getCoord().getX() - 100., (toNode.getCoord().getY() - 100. )));
						scenario.getNetwork().addNode(fromNode);
						
						link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(fromNode.getId() + "-to-" + toNode.getId()), fromNode, toNode);

					} else {
						// not the first stop facility
						
						// from node was already added to the network
						Node fromNode = scenario.getNetwork().getLinks().get(previousStopFacility.getLinkId()).getToNode();
						
						Node toNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId(lineId + "_" + routeNewId + "_" + transitStopFacilityNew.getId() + "_" + transitStopCounter), transitStopFacilityNew.getCoord());
						scenario.getNetwork().addNode(toNode);
						
						link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(fromNode.getId() + "-to-" + toNode.getId()), fromNode, toNode);
					}
					
					Set<String> modes = new HashSet<>();
					modes.add(TransportMode.car);
					link.setAllowedModes(modes);
					link.setCapacity(99999.);
					double beelineDistance = NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
					link.setLength(beelineDistance * trainBeelineDistanceFactor);

					double avgFreeSpeed = Double.NEGATIVE_INFINITY;
					
					if (lineNew.getName().equals("S3") || lineNew.getName().equals("S68")) {
						avgFreeSpeed = 14.69907402;
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S11")) {
						avgFreeSpeed = 18.74999993;
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S9") || lineNew.getName().equals("S8") || lineNew.getName().equals("S6") || lineNew.getName().equals("S1")) {
						avgFreeSpeed = 16.6666666; 
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S5")) {
						avgFreeSpeed = 12.5;
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S4") || lineNew.getName().equals("S28")) {
						avgFreeSpeed = 11.66666662;
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S7")) {
						avgFreeSpeed = 9.722222183;
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("S2")) {
						avgFreeSpeed = 8.3333333;
						log.info("S-Bahn --> line: " + lineNew.getName());
					
					} else if (lineNew.getName().equals("RB35")) {
						avgFreeSpeed = 17.97385635;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB52")) {
						avgFreeSpeed = 10.41666675;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB39")) {
						avgFreeSpeed = 11.1111112;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB54") || lineNew.getName().equals("RB44") || lineNew.getName().equals("RB43")) {
						avgFreeSpeed = 11.98743396;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB53") || lineNew.getName().equals("RB46") || lineNew.getName().equals("RB37") || lineNew.getName().equals("RB36") || lineNew.getName().equals("RB33") || lineNew.getName().equals("RB32") || lineNew.getName().equals("RB25")) {
						avgFreeSpeed = 13.888889;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB91") || lineNew.getName().equals("RB59")) {
						avgFreeSpeed = 16.6666668;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB51")) {
						avgFreeSpeed = 17.36111125;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB31")) {
						avgFreeSpeed = 18.51851867;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB45") || lineNew.getName().equals("RB40")) {
						avgFreeSpeed = 19.09722238;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB50") || lineNew.getName().equals("RB48") || lineNew.getName().equals("RB27")) {
						avgFreeSpeed = 19.4444446;
						log.info("RB --> line: " + lineNew.getName());	
					} else if (lineNew.getName().equals("RB34")) {
						avgFreeSpeed = 20.8333335;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB89")) {
						avgFreeSpeed = 22.43589762;
						log.info("RB --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RB69")) {
						avgFreeSpeed = 24.30555575;
						log.info("RB --> line: " + lineNew.getName());
						
					} else if (lineNew.getName().equals("RE14")) {
						avgFreeSpeed = 16.20370383;
						log.info("RE --> line: " + lineNew.getName());	
					} else if (lineNew.getName().equals("RE10") || lineNew.getName().equals("RE8")) {
						avgFreeSpeed = 19.09722238;
						log.info("RE --> line: " + lineNew.getName());	
					} else if (lineNew.getName().equals("RE13")) {
						avgFreeSpeed = 19.4444446;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE16")) {
						avgFreeSpeed = 19.67592608;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE42")) {
						avgFreeSpeed = 20.37037053;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE57") || lineNew.getName().equals("RE3") || lineNew.getName().equals("RE6") || lineNew.getName().equals("RE7")) {
						avgFreeSpeed = 20.8333335;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE17")) {
						avgFreeSpeed = 21.70138906;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE4")) {
						avgFreeSpeed = 22.2222224;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE19")) {
						avgFreeSpeed = 22.72727291;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE2")) {
						avgFreeSpeed = 23.6111113;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE5")) {
						avgFreeSpeed = 25.92592613;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE11")) {
						avgFreeSpeed = 27.08333355;
						log.info("RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("RE1")) {
						avgFreeSpeed = 26.14379106;
						log.info("RE --> line: " + lineNew.getName());
						
					} else if (lineNew.getName().equals("ICE78")) {
						avgFreeSpeed = 26.62037058;
						log.info("ICE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("ICE-43")) {
						avgFreeSpeed = 31.48148173;
						log.info("ICE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("ICE-42")) {
						avgFreeSpeed = 48.35390985;
						log.info("ICE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("ICE-41")) {
						avgFreeSpeed = 44.23868348;
						log.info("ICE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("ICE-10")) {
						avgFreeSpeed = 44.6428575;
						log.info("ICE --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("ICE-1")) {
						avgFreeSpeed = 35.714286;
						log.info("ICE --> line: " + lineNew.getName());
						
					} else if (lineNew.getName().equals("IC-50-MDV")) {
						avgFreeSpeed = 25.13227533;
						log.info("IC --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("IC-37") || lineNew.getName().equals("IC-32")) {
						avgFreeSpeed = 27.777778;
						log.info("IC --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("IC-55")) {
						avgFreeSpeed = 30.30303055;
						log.info("IC --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("EC-30-IC-30")) {
						avgFreeSpeed = 32.60869591;
						log.info("IC --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("IC-35")) {
						avgFreeSpeed = 38.01169621;
						log.info("IC --> line: " + lineNew.getName());
					} else if (lineNew.getName().equals("IC-31")) {
						avgFreeSpeed = 43.20987689;
						log.info("IC --> line: " + lineNew.getName());
					
					} else if (lineNew.getName().equals("Thalys")) {
						avgFreeSpeed = 22.8174605;
						log.info("Thalys --> line: " + lineNew.getName());
						
					} else if (lineNew.getName().equals("FLX20")) {
						avgFreeSpeed = 22.54428359;
						log.info("FLX20 --> line: " + lineNew.getName());
						
					} else {
						avgFreeSpeed = 11.111111;
						log.warn("Unknown transit line category: " + lineNew.getName() + " Route: "+ route.getId());
					}
					link.setFreespeed(avgFreeSpeed);
					
					log.info("Adding link " + link.getId() + " to the scenario.");
					scenario.getNetwork().addLink(link);
					
					linkIds.add(link.getId());
					
					TransitRouteStop stopNew = scenario.getTransitSchedule().getFactory().createTransitRouteStop(transitStopFacilityNew, departureOffset, departureOffset);
					stops.add(stopNew);
										
					scenario.getTransitSchedule().getFacilities().get(stopNew.getStopFacility().getId()).setLinkId(link.getId());
					
					double travelTime = link.getLength() / link.getFreespeed();
					departureOffset = departureOffset + travelTime;
					
					previousStopFacility = transitStopFacilityNew;
					transitStopCounter++;
				}
				
				Id<Link> firstLink = linkIds.get(0);
				Id<Link> lastLink = linkIds.get(linkIds.size() - 1);
				linkIds.remove(firstLink);
				linkIds.remove(linkIds.get(linkIds.size() - 1));
				
				NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(firstLink, linkIds, lastLink);				
				
				TransitRoute routeNew = scenario.getTransitSchedule().getFactory().createTransitRoute(routeNewId , networkRoute, stops, TransportMode.car);
				routeNew.setDescription(route.getId().toString().replaceAll("[^a-zA-Z0-9]", "-"));
				
				if (lineNew.getName().equals("S9")) {
					log.info("S9 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {

						if (t >= 19. * 3600) {
							headway = 30 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("S8")) {
					log.info("S8 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 1 * 3600.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {	
							headway = 3 * 3600.;
						}
						
						if (t >= 4. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 19. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 23. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("S7")) {
					log.info("S7 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 1 * 3600.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {	
							headway = 3 * 3600.;
						}
						
						if (t >= 4. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 20. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 23. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("S68")) {
					log.info("S68 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 19.; t = t + headway) {
						
						if (t >= 9. * 3600) {	
							headway = 7 * 3600.;
						}
						
						if (t >= 16. * 3600) {	
							headway = 20 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("S6")) {
					log.info("S6 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 1 * 3600.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {	
							headway = 3 * 3600.;
						}
						
						if (t >= 4. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 20. * 3600) {
							headway = 30 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("S5")) {
					log.info("S5 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 1 * 3600.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {	
							headway = 3 * 3600.;
						}
						
						if (t >= 4. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 23. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("S4")) {
					log.info("S4 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 1 * 3600.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 1. * 3600) {	
						headway = 3 * 3600.;
						}
					
						if (t >= 4. * 3600) {
						headway = 20 * 60.;
						}
					
						if (t >= 19. * 3600) {
						headway = 30 * 60.;
						}
					
						if (t >= 23. * 3600) {
						headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("S3")) {
					log.info("S3 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 18. * 3600) {
						headway = 30 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("S28")) {
					log.info("S28 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 20. * 3600) {	
						headway = 30 * 60.;
						}
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("S2")) {
				
					log.info("S2 --> line: " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 40 * 60.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 2. * 3600) {	
							headway = 120 * 60.;
						}
						
						if (t >= 4. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 6. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 20. * 3600) {
							headway = 30 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
				
				} else if (lineNew.getName().equals("S11")) {
					log.info("S11 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 19. * 3600) {	
							headway = 30 * 60.;
						}
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}

				} else if (lineNew.getName().equals("S1")) {
					
					log.info("S1 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 5. * 3600) {	
							headway = 20 * 60.;
						}
					
						if (t >= 7. * 3600) {
							headway = 10 * 60.;
						}
						
						if (t >= 8. * 3600) {
							headway = 20 * 60.;
						}
					
						if (t >= 19. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 23. * 3600) {
							headway = 60 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB91")) {
					log.info("RB91 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
				
				} else if (lineNew.getName().equals("RB89")) {
					log.info("RE89 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 21. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RB59")) {
					log.info("RB59 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0.; t <= 3600 * 24.; t = t + headway) {
					
						if (t >= 1. * 3600) {	
							headway = 4 * 3600.;
						}
					
						if (t >= 5. * 3600) {	
							headway = 30 * 60.;
						}
					
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB54") || lineNew.getName().equals("RB69")) {
					log.info("RB 54,69 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 21.; t = t + headway) {
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
				
				} else if (lineNew.getName().equals("RB53")) {
					log.info("RB53 --> line: " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
					
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB52")) {
					log.info("RB52 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB51")) {
					log.info("RB51 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 21. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
			
				} else if (lineNew.getName().equals("RB50")) {
					log.info("RB50 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB48")) {
					log.info("RB48 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB46")) {
					log.info("RB46 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 5. * 3600) {	
							headway = 30 * 60.;
						}
						
						if (t >= 21. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB45")) {
					log.info("RB45 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB44")) {
					log.info("RB44 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}		
				} else if (lineNew.getName().equals("RB43")) {
					log.info("RB43 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 22.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
						
				} else if (lineNew.getName().equals("RB40")) {
					log.info("RB40 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {	
							headway = 4 * 3600.;
						}
						
						if (t >= 5. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}		
					
				} else if (lineNew.getName().equals("RB39")) {
					log.info("RB39 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB37")) {
					log.info("RB37 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 8. * 3600) {	
							headway = 60 * 60.;
						}
						
						if (t >= 13. * 3600) {	
							headway = 30 * 60.;
						}
						
						if (t >= 15. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB36")) {
					log.info("RB36 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}		
	
				} else if (lineNew.getName().equals("RB35")) {
					log.info("RB35 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 20.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB34")) {
					log.info("RB34 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB33")) {
					log.info("RB33 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB32")) {
					log.info("RB32 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 22.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().equals("RB31")) {
					log.info("RB31 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
						
				} else if (lineNew.getName().equals("RB27")) {
					log.info("RB27 --> line: " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 19.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RB25")) {
					log.info("RB25 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 5. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 10. * 3600) {
							headway = 30 * 60.;
						}
						
						if (t >= 21. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter
						++;
					}
					
//				route split for line RE57
				} else if (lineNew.getName().equals("RE57") && route.getId().toString().contains("Brilon")) {
					log.info("RE57B --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {
							headway = 480 * 60.;
						}
						
						if (t >= 9. * 3600) {
							headway = 120 * 60.;
						}
						
						if (t >= 15. * 3600) {
							headway = 60 * 60.;
						}
						
						if (t >= 19. * 3600) {
							headway = 120 * 60.;
						}
						if (t >= 21. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RE57")) {
					log.info("RE57W --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 18.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().equals("RE5")) {
					log.info("RE5 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}		
					
				} else if (lineNew.getName().equals("RE42")) {
					log.info("RE42 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE2")){
					log.info("RE2 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 1. * 3600) {
							headway = 3 * 3600.;
						}
						
						if (t >= 4. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE19")) {
					log.info("RE19 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE17")) {
					log.info("RE17 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 22; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE16")) {
					log.info("RE16 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 19; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE14")) {
					log.info("RE14 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600; t <= 3600 * 23; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE13")) {
					log.info("RE13 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE11")) {
					log.info("RE11 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 22.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE10")) {
					log.info("RE10 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
							
						if ( t >= 19. * 3600) {
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("RE1") || lineNew.getName().equals("RE3") || lineNew.getName().equals("RE4") || lineNew.getName().equals("RE6")
							|| lineNew.getName().equals("RE7") || lineNew.getName().equals("RE8")) {
						log.info("RE 1,3,4,6,7,8 --> line: " + lineNew.getName());
						
						int depCounter = 0;
						double headway = 60 * 60.;
						for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
							
							routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
							depCounter++;	
							
						}
						
				} else if (lineNew.getName().equals("ICE78")) {
					log.info("ICE-78 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 19.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}	
						
				} else if (lineNew.getName().equals("ICE-43")) {
					log.info("ICE-43 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 20.; t = t + headway) {
							
						if (t >= 9. * 3600) {
							headway = 120 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("ICE-42")) {
					log.info("ICE-42 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						if (t >= 19. * 3600) {
							headway = 60 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("ICE-41")) {
					log.info("ICE-41 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 2 * 3600.; t <= 3600 * 21.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("ICE-10")) {
					log.info("ICE-10 --> line " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 20.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("ICE-1")) {
					log.info("ICE-1 --> line " + lineNew.getName());
								
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 6.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("IC-55")) {
					log.info("IC-55 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 3 * 3600.; t <= 3600 * 15.; t = t + headway) {
														
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("IC-50-MDV")) {
					log.info("IC-50-MDV --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 9 * 3600.; t <= 3600 * 15.; t = t + headway) {
							
						if (t >= 11. * 3600) {
							headway = 240 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("IC-37")) {
					log.info("IC-37 --> line " + lineNew.getName());
								
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 12 * 3600.; t <= 3600 * 12.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("IC-35")) {
					log.info("IC-35 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 1 * 3600.; t <= 3600 * 15.; t = t + headway) {
							
						if (t >= 4. * 3600) {
							headway = 120 * 60.;
						}
							
						if (t >= 6. * 3600) {
							headway = 60 * 60.;
						}
								
						if (t >= 7. * 3600) {
							headway = 180 * 60.;
						}
							
						if (t >= 10. * 3600) {
							headway = 120 * 60.;
						}
													
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
//				route split for line IC-32
				} else if (lineNew.getName().equals("IC-32") && route.getId().toString().contains("Aachen")) {
					log.info("IC-32a --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 300 * 60.;
					for (double t = 7 * 3600.; t <= 3600 * 14.; t = t + headway) {
						
						if (t >= 12 * 3600) {
							headway = 120 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
							
				} else if (lineNew.getName().equals("IC-32")) {
					log.info("IC-32 --> line " + lineNew.getName());
				
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 3 * 3600.; t <= 3600 * 19.; t = t + headway) {
							
						if (t >= 5. * 3600) {
							headway = 60 * 60.;
						}
							
						if (t >= 10. * 3600) {
							headway = 120 * 60.;
						}
							
						if (t >= 12. * 3600) {
							headway = 60 * 60.;
						}
							
						if (t >= 14. * 3600) {
							headway = 180 * 60.;
						}
							
						if (t >= 17. * 3600) {
							headway = 60 * 60.;
						}
		
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("IC-31")) {
					log.info("IC-31 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 16.; t = t + headway) {
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
//				route split for line IC-30
				} else if (lineNew.getName().equals("EC-30-IC-30") && route.getId().toString().contains("Zürich")) {
					log.info("EC-30 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 180 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 7.; t = t + headway) {
					
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("EC-30-IC-30")) {
					log.info("IC-30 --> line " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
							
						if (t >= 8. * 3600) {
							headway = 60 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
						
				} else if (lineNew.getName().equals("Thalys")) {
					log.info("Thalys --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 120 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 18.; t = t + headway) {
							
						if (t >= 8. * 3600) {
							headway = 240 * 60.;
						}
							
						if (t >= 16. * 3600) {
							headway = 120 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
//				FlixBus FLX20 from Hamburg to Cologne
				} else if (lineNew.getName().equals("FLX20")) {
					log.info("FLX20 --> line " + lineNew.getName());
							
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 14.; t = t + headway) {
							
						if (t >= 7. * 3600) {
							headway = 420 * 60.;
						}
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));
						depCounter++;
					}
					
				} else if (lineNew.getName().equals("6283865") || lineNew.getName().equals("6290015")) {
					log.warn("Skipping " + lineNew.getName().toString());
					
				} else {
					log.warn("Unknown transit line category: " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {				
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
				}
				
				lineNew.addRoute(routeNew);
				routeCounter++;
			}
			scenario.getTransitSchedule().addTransitLine(lineNew);
			lineCounter++;
		}
				
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputScheduleFile);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(outptTransitVehicleFile);
		new NetworkWriter(scenario.getNetwork()).write(outputNetworkFile);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}

	private Departure createVehicleAndReturnDeparture(int routeCounter, TransitRoute routeNew, VehicleType type, double depCounter, double t) {
		Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId(vehicleCounter + "_" + lineCounter + "_" + routeCounter + "_" + routeNew.getDescription()), type);
		vehicleCounter++;
		scenario.getTransitVehicles().addVehicle(vehicle);
		
		Departure departure = scenario.getTransitSchedule().getFactory().createDeparture(Id.create(routeNew.getId() + "_" + depCounter, Departure.class), t);
		departure.setVehicleId(vehicle.getId());	
		return departure;
	}

}