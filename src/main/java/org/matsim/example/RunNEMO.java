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

package org.matsim.example;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.vsp.andreas.osmBB.extended.TransitScheduleImpl;

/**
* @author ikaddoura
*/

public class RunNEMO {

	private static final Logger log = Logger.getLogger(RunNEMO.class);

	public static void main(String[] args) {
		
		RunNEMO runner = new RunNEMO();
		runner.run();
	}

	public void run() {
		
		final String network = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network_with-pt.xml.gz";
		final String transitSchedule = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/transitSchedule_GTFS.xml.gz";
		final String transitVehicles = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/transitVehicles_GTFS.xml.gz";
		
		final String adjustedNetwork = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network_with-pt_adjusted.xml.gz";
		final String adjustedTransitSchedule = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/transitSchedule_GTFS_adjusted.xml.gz";
		final String outputDirectory = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/pt-visualization/";
		
		{
			Config config = ConfigUtils.createConfig();
			
			config.plans().setInputFile(null);
			config.network().setInputFile(network);
			config.transit().setUseTransit(true);
			config.transit().setTransitScheduleFile(transitSchedule);
			config.transit().setVehiclesFile(transitVehicles);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			// correct network
			
			for (Link link : scenario.getNetwork().getLinks().values()) {
				
				if (link.getLength() > 0 && link.getLength() < Double.POSITIVE_INFINITY) {
					// ok
				} else {
					log.warn("adjust link length for link " + link.getId());
					link.setLength(1.234);
				}
			}
			
			// correct schedule

			List<Id<TransitStopFacility>> wrongStopIDs = new ArrayList<>();
			List<Id<TransitLine>> linesWithWrongStopIDs = new ArrayList<>();

			for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
				
				if (stop.getCoord().getX() > Double.NEGATIVE_INFINITY && stop.getCoord().getX() < Double.POSITIVE_INFINITY && 
						stop.getCoord().getY() > Double.NEGATIVE_INFINITY && stop.getCoord().getY() < Double.POSITIVE_INFINITY) {
					// probably ok
					
				} else {
					wrongStopIDs.add(stop.getId());
				}
			}
			
			// get lines for these stops
			for (Id<TransitStopFacility> id : wrongStopIDs) {
				for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
					for (TransitRoute route : line.getRoutes().values()) {
						for (TransitRouteStop stop : route.getStops()) {
							if (stop.getStopFacility().getId().toString().equals(id.toString())) {
								linesWithWrongStopIDs.add(line.getId());
							}
						}
					}
				}
			}
					
			TransitSchedule tS = makeTransitScheduleModifiable(scenario.getTransitSchedule());
			
			// remove stops
			for (Id<TransitStopFacility> id : wrongStopIDs) {
				log.warn("Removing stop Id " + id);
				tS.getFacilities().remove(id);
			}
			
			// remove lines
			for (Id<TransitLine> id : linesWithWrongStopIDs) {
				log.warn("Removing transit line " + id);
				tS.getTransitLines().remove(id);
			}
			
			NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
			writer.write(adjustedNetwork);
			
			TransitScheduleWriterV1 tSWriter = new TransitScheduleWriterV1(tS);
			tSWriter.write(adjustedTransitSchedule);
				
		}
		
		{
			Config config = ConfigUtils.createConfig();
			
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setRunId("nemo-pt-visualization");
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			
			config.qsim().setEndTime(24 * 3600.);
			config.qsim().setStartTime(0.);
			
			config.plans().setInputFile(null);
			config.network().setInputFile(adjustedNetwork);
			config.transit().setUseTransit(true);
			config.transit().setTransitScheduleFile(adjustedTransitSchedule);
			config.transit().setVehiclesFile(transitVehicles);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
			controler.run();
		
			log.info("Done.");
		}
		
	}
	
	private static TransitSchedule makeTransitScheduleModifiable(TransitSchedule transitSchedule){
		TransitSchedule tS = new TransitScheduleImpl(transitSchedule.getFactory());
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			tS.addStopFacility(stop);			
		}
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			tS.addTransitLine(line);
		}
		
		return tS;
	}

}

