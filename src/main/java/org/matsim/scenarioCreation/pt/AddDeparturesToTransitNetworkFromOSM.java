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
import org.matsim.core.utils.geometry.geotools.MGC;
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
		final String projectDirectory = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/";
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
		
		final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", "EPSG:31464");
		
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
					
					if (lineNew.getName().contains("S")) {
						avgFreeSpeed = 8.3333333; // 30 km/h
						log.info("S-Bahn --> line: " + lineNew.getName());
					} else if (lineNew.getName().contains("RB") || lineNew.getName().contains("RE")) {
						avgFreeSpeed = 13.888889; // 50 km/h
						log.info("RB / RE --> line: " + lineNew.getName());
					} else if (lineNew.getName().contains("IC") || lineNew.getName().contains("Thalys")) {
						avgFreeSpeed = 27.777778; // 100 km/h
						log.info("IC / ICE / Thalys --> line: " + lineNew.getName());
					} else {
						avgFreeSpeed = 11.111111; // 40 km/h
						log.warn("Unknown transit line category: " + lineNew.getName());
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
				
				if (lineNew.getName().contains("S1")) {
					
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
							
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().contains("S2")) {
						
					log.info("S2 --> line: " + lineNew.getName());
						
					int depCounter = 0;
					double headway = 40 * 60.;
					for (double t = 0; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 2. * 3600) {	
							headway = 120 * 60.;
						}
						
						if (t >= 4. * 3600) {
							headway = 40 * 60.;
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
						
				} else if (lineNew.getName().contains("S3")) {
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
					
				} else if (lineNew.getName().contains("S4")) {
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
					
				} else if (lineNew.getName().contains("S5")) {
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
					
				} else if (lineNew.getName().contains("S6")) {
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
					
				} else if (lineNew.getName().contains("S7")) {
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
					
				} else if (lineNew.getName().contains("S8")) {
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
					
				} else if (lineNew.getName().contains("S9")) {
					log.info("S9 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 20 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						if (t >= 19. * 3600) {	
							headway = 30 * 60.;
						}
						
						if (t >= 4. * 3600) {
							headway = 20 * 60.;
						}
						
						if (t >= 19. * 3600) {
							headway = 30 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().contains("S11")) {
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
					
				} else if (lineNew.getName().contains("S28")) {
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
					
				} else if (lineNew.getName().contains("S68")) {
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
					
				} else if (lineNew.getName().contains("RB27")) {
					log.info("RB27 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 19.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB31")) {
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
					
				} else if (lineNew.getName().contains("RB32")) {
					log.info("RB32 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 22.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB33")) {
					log.info("RB33 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB34")) {
					log.info("RB34 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	

				} else if (lineNew.getName().contains("RB35")) {
					log.info("RB35 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 20.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB36")) {
					log.info("RB36 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
				
				} else if (lineNew.getName().contains("RB37")) {
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
					
				} else if (lineNew.getName().contains("RB39")) {
					log.info("RB39 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 30 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						if (t >= 20. * 3600) {	
							headway = 60 * 60.;
						}
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB40")) {
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

				} else if (lineNew.getName().contains("RB43")) {
					log.info("RB43 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}	
					
				} else if (lineNew.getName().contains("RB44")) {
					log.info("RB44 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 23.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB45")) {
					log.info("RB45 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 6 * 3600.; t <= 3600 * 21.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB46")) {
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
					
				} else if (lineNew.getName().contains("RB48")) {
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
					
				} else if (lineNew.getName().contains("RB50")) {
					log.info("RB50 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB51")) {
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

				} else if (lineNew.getName().contains("RB52")) {
					log.info("RB52 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				} else if (lineNew.getName().contains("RB53")) {
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
					
				} else if (lineNew.getName().contains("RB59")) {
					log.info("RB59 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 0 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
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
					
				} else if (lineNew.getName().contains("RB91")) {
					log.info("RB59 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 5 * 3600.; t <= 3600 * 24.; t = t + headway) {
						
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
				
				} else if (lineNew.getName().contains("RE1") || lineNew.getName().contains("RE3") || lineNew.getName().contains("RE6")
						|| lineNew.getName().contains("RE7") || lineNew.getName().contains("RE8")) {
					log.info("RE 1,3,6,7,8 --> line: " + lineNew.getName());
					
					int depCounter = 0;
					double headway = 60 * 60.;
					for (double t = 4 * 3600.; t <= 3600 * 24.; t = t + headway) {
						routeNew.addDeparture(createVehicleAndReturnDeparture(routeCounter, routeNew, type, depCounter, t));				
						depCounter++;						
					}
					
				// TODO: continue with other transit lines
				// TODO: use something else than 'contains' to differentiate between e.g. RE2 and RE27...
					
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

