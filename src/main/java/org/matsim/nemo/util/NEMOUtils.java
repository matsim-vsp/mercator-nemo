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

package org.matsim.nemo.util;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

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
