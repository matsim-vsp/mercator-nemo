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

package org.matsim.scenarioCalibration.marginals;

import java.util.Arrays;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * Created by amit on 02.03.18.
 */

public class PrepareConfig {
    private static final String config_for_activityParams = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/config_take_activity-parametersOnly.xml";
    public static final boolean rideAsMainMode = false;

    public static void main(String[] args) {

        String outConfigFile ;
        String vehicleFile ;
        if (rideAsMainMode) {
            outConfigFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/preparedConfig_rideAsMainMode.xml";
            vehicleFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/modeVehicleTypes.xml";
        } else {
            outConfigFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/preparedConfig.xml";
        }

        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = ConfigUtils.loadConfig(config_for_activityParams).planCalcScore();

        Config outConfig = ConfigUtils.createConfig();

        outConfig.counts().setInputFile("NemoCounts_data_allCounts_Pkw_24Nov2017_filteredForRuhr.xml");
        outConfig.counts().setWriteCountsInterval(10);
        outConfig.counts().setCountsScaleFactor( (1 / NEMOUtils.SAMPLE_SIZE) * NEMOUtils.RUHR_CAR_SHARE / (NEMOUtils.RUHR_CAR_SHARE + NEMOUtils.RUHR_PT_SHARE) );
        outConfig.counts().setWriteCountsInterval(10);
        outConfig.counts().setOutputFormat("all");

        outConfig.plans().setInputFile("plans_1pct_fullChoiceSet_coordsAssigned_splitActivities_filteredForRuhr.xml.gz");
        outConfig.plans().setRemovingUnneccessaryPlanAttributes(true);

        outConfig.network().setInputFile("tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz");

        outConfig.global().setNumberOfThreads(4);
        outConfig.global().setCoordinateSystem(NEMOUtils.NEMO_EPSG);
        outConfig.global().setInsistingOnDeprecatedConfigVersion(false);

        double flowCapFactor = NEMOUtils.SAMPLE_SIZE * (NEMOUtils.RUHR_CAR_SHARE + NEMOUtils.RUHR_PT_SHARE) / NEMOUtils.RUHR_CAR_SHARE;
        outConfig.qsim().setFlowCapFactor(NEMOUtils.round(flowCapFactor, 3));//TODO why 1% only
        outConfig.qsim().setStorageCapFactor( NEMOUtils.round(     flowCapFactor / Math.pow(flowCapFactor, 0.25)   ,3)  );
        outConfig.qsim().setStuckTime(30.0);
        outConfig.qsim().setUsingFastCapacityUpdate(true);
        outConfig.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        outConfig.qsim().setEndTime(30*3600.);

        if (rideAsMainMode) {
            outConfig.qsim().setMainModes(Arrays.asList(TransportMode.car, TransportMode.ride));
            outConfig.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

            //prepare mode vehicle type
            Vehicles vehs = VehicleUtils.createVehiclesContainer();
            {
                VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
                vehs.addVehicleType(car);

                VehicleType ride = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.ride, VehicleType.class));
                ride.setPcuEquivalents(0.);
                vehs.addVehicleType(ride);

                new VehicleWriterV1(vehs).writeFile(vehicleFile);
            }
            outConfig.vehicles().setVehiclesFile("modeVehicleTypes.xml");
        }

        outConfig.plansCalcRoute().setNetworkModes(Arrays.asList(TransportMode.car, TransportMode.ride));
        outConfig.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.bike).setBeelineDistanceFactor(1.3);
        outConfig.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.walk).setBeelineDistanceFactor(1.3);
        outConfig.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.bike).setTeleportedModeSpeed(5.56); //faster -> more attractive
        outConfig.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.walk).setTeleportedModeSpeed(1.39); //faster -> more attractive

        outConfig.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
        outConfig.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);

        if ( rideAsMainMode ) {
            outConfig.travelTimeCalculator().setSeparateModes(true);
            outConfig.travelTimeCalculator().setAnalyzedModes("car,ride");
        } else{
            //using overriding modules
        }

        outConfig.planCalcScore().setWriteExperiencedPlans(true);
        outConfig.planCalcScore().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(0.);
        outConfig.planCalcScore().getOrCreateModeParams(TransportMode.bike).setMarginalUtilityOfTraveling(0.);
        outConfig.planCalcScore().getOrCreateModeParams(TransportMode.walk).setMarginalUtilityOfTraveling(0.);
        outConfig.planCalcScore().getOrCreateModeParams(TransportMode.ride).setMarginalUtilityOfTraveling(0.);
        outConfig.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);
        planCalcScoreConfigGroup.getActivityParams().forEach(pm -> outConfig.planCalcScore().addActivityParams(pm));

        outConfig.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
        reRoute.setWeight(0.15);
        outConfig.strategy().addStrategySettings(reRoute);

        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name());
        modeChoice.setWeight(0.15);
        outConfig.strategy().addStrategySettings(modeChoice);
        outConfig.subtourModeChoice().setModes(new String[]{TransportMode.car, TransportMode.bike, TransportMode.walk, TransportMode.ride});
        outConfig.subtourModeChoice().setChainBasedModes(new String[]{TransportMode.car, TransportMode.bike});

        StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.70);
        outConfig.strategy().addStrategySettings(changeExpBeta);
        outConfig.strategy().setMaxAgentPlanMemorySize(8);

        outConfig.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
        outConfig.vspExperimental().setWritingOutputEvents(true);

        new ConfigWriter(outConfig).write(outConfigFile);
    }
}
