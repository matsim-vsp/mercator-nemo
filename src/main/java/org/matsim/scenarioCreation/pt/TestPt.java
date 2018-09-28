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

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

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
		// final String projectDirectory = "C://Users//Gregor//Documents//VSP_Arbeit";
		
		//Config config = ConfigUtils.createConfig();
		Config config = ConfigUtils.loadConfig(projectDirectory + "data/matsim_input/2018-05-28_shorterIntraZonalDist/preparedConfig_TestPt.xml");
		//Config config = ConfigUtils.loadConfig("C://Users//Gregor//Documents//VSP_Arbeit//2018-05-28_shorterIntraZonalDist//config_take_activity-parametersOnly.xml");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		//config.controler().setOutputDirectory("C:/Users/Gregor/Documents/VSP_Arbeit/Nemo/OutputNemoTest");
		config.controler().setOutputDirectory(projectDirectory + "data/pt/OSM_GTFS_merged_final/nemo-merged-gtfs-osm-pt-visualization_test_IK/");
		
		config.controler().setRunId("gtfs-osm");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		
		config.qsim().setEndTime(30 * 3600.);
		config.qsim().setStartTime(0.);
		
//		config.plans().setInputFile("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//InputNemoTest//mytestpopulation_1.xml");
//		config.network().setInputFile("C:/Users/Gregor/Documents/VSP_Arbeit/Nemo/InputNemoTest/network_only_Pt_and_car.xml");
		config.transit().setUseTransit(true);
//		config.transit().setTransitScheduleFile(projectDirectory + "/pt/OSM_GTFS_merged_final/transitSchedule_GTFS_OSM.xml.gz");
//		config.transit().setVehiclesFile(projectDirectory + "/pt/OSM_GTFS_merged_final/transitVehicles_GTFS_OSM.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// use the sbb pt raptor router
		controler.addOverridingModule(new org.matsim.core.controler.AbstractModule() {
			
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		controler.run();

		log.info("Done.");
	}

}

