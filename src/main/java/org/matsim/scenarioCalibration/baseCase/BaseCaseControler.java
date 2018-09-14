/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.scenarioCalibration.baseCase;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

/**
 * Created by amit on 09.07.18.
 */

public class BaseCaseControler {


    public static void main (String[] args) {

        String configFile = "C://Users//Gregor//Documents/VSP_Arbeit//Nemo//InputNemoTest//preparedConfig_baseCase.xml/";
        String runId = "test123";
        String outputDir = "C://Users//Gregor//Documents/VSP_Arbeit//Nemo//OutputNemoTest";

        if (args.length>0) {
            configFile = args[0];
            runId = args[1];
            outputDir = args[2];
        }

        Config config = ConfigUtils.loadConfig(configFile);

        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDir);
        config.plansCalcRoute().setInsertingAccessEgressWalk(true); // so that bicycle can be route from car link to bike links.
        config.network().setInputFile("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//InputNemoTest//network_only_Pt_and_car.xml");
        config.plans().setInputFile("C://Users//Gregor//Documents//VSP_Arbeit//Nemo//InputNemoTest//mytestpopulation.xml/");
        config.transit().setTransitScheduleFile("C:/Users/Gregor/Documents/VSP_Arbeit//Nemo/InputNemoTest/transitSchedule_GTFS_adjusted.xml.gz");
        config.transit().setVehiclesFile("C:/Users/Gregor/Documents/VSP_Arbeit/Nemo/InputNemoTest/transitVehicles_GTFS.xml.gz");

        //vspDefaults
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

        if (args.length==0) {
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        //ride gets network routes but teleported.
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addTravelTimeBinding("ride").to(networkTravelTime());
                this.addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
            }
        });

        //analysis
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();
    }
}
