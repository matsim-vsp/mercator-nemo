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

package org.matsim.scenarioCalibration.modeChoice;

import java.util.Arrays;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;

/**
 *
 * Modal share: bike, car, pt, walk -= 8,53,16,23
 *
 * Created by amit on 15.02.18.
 */

public class RuhrModeChoiceCalibrationControler {

    //taken from run9/run9.output_config.xml --> flowCapFactor 0.01; countScaleFactor 100;
    private static String configFile = "../../repos/runs-svn/nemo/modeChoice/input/config.xml";

    // use FilterNRWPopulationForRuhr to filter plans after location chocie
    private static final String plansFile = "plans_1pct_Ruhr_noRoutes.xml.gz";

    // use FilterNRWCountsForRuhe to filter counts in Ruhr region
    private static final String countsFile = "counts_1pct_Ruhr.xml.gz";

    // a detailed network
    private static final String networkFile = "detailedRuhr_Network_17022018filteredcleaned_network.xml.gz";

    public static void main(String[] args) {
        String runId = "testCalib";
        String outputDir = "../../repos/runs-svn/nemo/modeChoice/";

        double carASC = -0.5;
        double ptASC = -0.7;
        double bikeASC = 0.0;
        double bikeMargUtilDist = 0.;
        double walkMargUtilDist = 0.;

        if (args.length >0) {
            configFile = args[0];
            runId = args[1];
            outputDir = args[2];
            bikeASC = Double.valueOf(args[3]);
            carASC = Double.valueOf(args[4]);
            ptASC = Double.valueOf(args[5]);
            bikeMargUtilDist = Double.valueOf(args[6]);
            walkMargUtilDist = Double.valueOf(args[7]);
        }

        if ( ! outputDir.contains(runId) ) {
            outputDir = outputDir.endsWith("/") ? outputDir+runId+"/" : outputDir+"/"+runId+"/";
        }

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(plansFile);
        config.network().setInputFile(networkFile);
//        config.counts().setInputFile(countsFile);

        if (args.length==0){
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        }


        config.controler().setOutputDirectory(outputDir);
        config.controler().setRunId(runId);

        config.qsim().setMainModes(Arrays.asList(TransportMode.car, TransportMode.bike));
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles); // TODO: queue/withHoles/KWM ?
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setEndTime(30*3600.);

        config.plansCalcRoute().setNetworkModes(Arrays.asList(TransportMode.car,TransportMode.bike)); //TODO ptSlow and ptFast
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes("car,bike");

        // strategies (subtourModeChoice 0.15, reRoute 0.1, timeMutation 0.05) already inside config.

        // asc for bike and walk  are zero; if necessary, use marginalUtilDist
        config.planCalcScore().getOrCreateModeParams(TransportMode.bike).setConstant(bikeASC);
        config.planCalcScore().getOrCreateModeParams(TransportMode.bike).setMarginalUtilityOfDistance(bikeMargUtilDist);
        config.planCalcScore().getOrCreateModeParams(TransportMode.walk).setConstant(0.);
        config.planCalcScore().getOrCreateModeParams(TransportMode.walk).setMarginalUtilityOfDistance(walkMargUtilDist);
        config.planCalcScore().getOrCreateModeParams(TransportMode.car).setConstant(carASC);
        config.planCalcScore().getOrCreateModeParams(TransportMode.pt).setConstant(ptASC);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();
        {
            VehicleType car = vehicles.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
            car.setMaximumVelocity(100.0/3.6); //100 kph
            car.setPcuEquivalents(1.0);
            vehicles.addVehicleType(car);
        }
        {
            VehicleType bike = vehicles.getFactory().createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
            bike.setMaximumVelocity(15.0/3.6); // 15 kph
            bike.setPcuEquivalents(0.25);
            vehicles.addVehicleType(bike);
        }

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);

                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });
        controler.run();
    }
}