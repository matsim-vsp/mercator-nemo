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

package org.matsim.nemo.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class RunPt {
	
	public static void main(String[] args) {
		
		RunPt runner = new RunPt();
		runner.run();
	}

	public void run() {
		
		// adjust these directories
		final String projectDirectory = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/";
		final String directory = projectDirectory + "data/pt/extendedScheduleFromOSM/";
		
		final String outputScheduleFile = directory + "OSMtransitSchedule.xml";
		final String outptTransitVehicleFile = directory + "OSMtransitVehicles.xml";
		final String outputNetworkFile = directory + "OSMtransitNetwork.xml";
				
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(directory + "/run_test/");
		config.controler().setRunId("osm-pt-v1");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		
		config.qsim().setEndTime(30. * 3600.);
		config.qsim().setStartTime(0.);
		
		config.plans().setInputFile(null);
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(outputScheduleFile);
		config.transit().setVehiclesFile(outptTransitVehicleFile);
		config.network().setInputFile(outputNetworkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}

}

