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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class TestPt {


	private static final Logger log = Logger.getLogger(TestPt.class);

	public static void main(String[] args) {
		
		TestPt runner = new TestPt();
		runner.run();
	}

	public void run() {
		
		final String projectDirectory = "../shared-svn/projects/nemo_mercator/";

		Config config = ConfigUtils.createConfig();
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(projectDirectory + "data/pt/OSM_GTFS_merged_final/nemo-merged-gtfs-osm-pt-visualization_test2/");
		config.controler().setRunId("gtfs-osm");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		
		config.qsim().setEndTime(30 * 3600.);
		config.qsim().setStartTime(0.);
		
		config.plans().setInputFile(null);
		config.network().setInputFile(projectDirectory + "data/pt/OSM_GTFS_merged_final/detailedRuhr_Network_10072018filtered_network_GTFS_OSM.xml.gz");
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(projectDirectory + "data/pt/OSM_GTFS_merged_final/transitSchedule_GTFS_OSM.xml.gz");
		config.transit().setVehiclesFile(projectDirectory + "data/pt/OSM_GTFS_merged_final/transitVehicles_GTFS_OSM.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	
		log.info("Done.");
		
		
	}

}

