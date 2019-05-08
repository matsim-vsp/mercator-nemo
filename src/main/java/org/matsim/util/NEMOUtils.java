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

package org.matsim.util;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by amit on 29.01.18.
 */

public final class NEMOUtils {

    public static final String TRANSIT_NETWORK_PREFIX = "pt_";

    public static final String NEMO_EPSG = "EPSG:25832";

    /**
     * Returns scenario containing only plans file location.
     */
    public static Scenario loadScenarioFromPlans(String plansFile) {
        Config config = new Config();
        config.addCoreModules();
        config.plans().setInputFile(plansFile);
        config.global().setCoordinateSystem(NEMOUtils.NEMO_EPSG);
        return ScenarioUtils.loadScenario(config);
    }

    public static DistanceDistribution getDistanceDistributionFromMiD(double countScaleFactor, PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
        DistanceDistribution distanceDistribution = new DistanceDistribution();
        Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> modeRoutingParamsMap = plansCalcRouteConfigGroup.getModeRoutingParams();

        distanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.car, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.car, 1.3));
        distanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.pt, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.pt, 1.3));
        distanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.bike, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.bike, 1.3));
        distanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.walk, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.walk, 1.3));
        distanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.ride, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.ride, 1.3));

        distanceDistribution.setModeToScalingFactor(TransportMode.car, countScaleFactor);
        distanceDistribution.setModeToScalingFactor(TransportMode.pt, countScaleFactor);
        distanceDistribution.setModeToScalingFactor(TransportMode.bike, countScaleFactor);
        distanceDistribution.setModeToScalingFactor(TransportMode.ride, countScaleFactor);
        distanceDistribution.setModeToScalingFactor(TransportMode.walk, countScaleFactor);

        // we are using extended MiD data for the Ruhrgebiet area. Data can be found in shared-svn\projects\nemo_mercator\data\original_files\MID\MiD2017_Wege_RVR-Gebiet_mit_Berechnungen.xlsx
        distanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(0, 1000.), 10000, 357488);
        distanceDistribution.addToDistribution(TransportMode.pt, new DistanceBin.DistanceRange(0, 1000), 10000, 28540);
        distanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(0.0, 1000.), 10000, 212908);
        distanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(0.0, 1000.), 10000, 1842358);
        distanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(0.0, 1000.), 1000, 124379);

        distanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(1000.0, 3000.), 10000, 1213004);
        distanceDistribution.addToDistribution(TransportMode.pt, new DistanceBin.DistanceRange(1000.0, 3000.), 10000, 437762);
        distanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(1000.0, 3000.), 10000, 541548);
        distanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(1000.0, 3000.), 10000, 832557);
        distanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(1000.0, 3000.), 1000, 483180);

        distanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(3000.0, 5000.), 10000, 842537);
        distanceDistribution.addToDistribution(TransportMode.pt, new DistanceBin.DistanceRange(3000.0, 5000.), 10000, 323085);
        distanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(3000.0, 5000.), 10000, 328707);
        distanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(3000.0, 5000.), 10000, 132547);
        distanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(3000.0, 5000.), 1000, 232588);

        distanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(5000.0, 10000.), 10000, 1274829);
        distanceDistribution.addToDistribution(TransportMode.pt, new DistanceBin.DistanceRange(5000.0, 10000.), 10000, 326567);
        distanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(5000.0, 10000.), 10000, 99804);
        distanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(5000.0, 10000.), 10000, 68559);
        distanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(5000.0, 10000.), 1000, 325927);

        distanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(10000.0, 1000000.), 10000, 1979366);
        distanceDistribution.addToDistribution(TransportMode.pt, new DistanceBin.DistanceRange(10000.0, 1000000.), 10000, 456848);
        distanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(10000.0, 1000000.), 10000, 63259);
        distanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(10000.0, 1000000.), 10000, 33138);
        distanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(10000.0, 1000000.), 1000, 322899);

        return distanceDistribution;
    }

    private static double getBeelineDistanceFactor(
            Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> modeRoutingParams,
            String modeKey,
            double defaultValue) {
        return modeRoutingParams.containsKey(modeKey) ? modeRoutingParams.get(modeKey).getBeelineDistanceFactor() : defaultValue;
    }

    public static AbstractModule createModalShareAnalysis() {
        return new AbstractModule() {
            @SuppressWarnings("PointlessBinding")
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        };
    }

    public static List<PlanCalcScoreConfigGroup.ActivityParams> createTypicalDurations(String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds) {

        List<PlanCalcScoreConfigGroup.ActivityParams> result = new ArrayList<>();
        for (long duration = minDurationInSeconds; duration <= maxDurationInSeconds; duration += durationDifferenceInSeconds) {
            final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(type + "_" + duration + ".0");
            params.setTypicalDuration(duration);
            result.add(params);
        }
        return result;
    }
}
