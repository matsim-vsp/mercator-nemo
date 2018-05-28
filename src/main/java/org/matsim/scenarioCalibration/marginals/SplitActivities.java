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

import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;
import playground.vsp.openberlinscenario.planmodification.SplitActivityTypesBasedOnDuration;

/**
 * Created by amit on 01.03.18.
 */

public class SplitActivities {

    public static void main(String[] args) {

        final String inputPopulationFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_stayHomePlans.xml.gz";

        final String outputPopulationFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_splitActivities.xml.gz";
        final String outputConfigFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/config_take_activity-parametersOnly.xml";

        final double timeBinSize_s = 600.;
        final String[] activities = {ActivityTypes.HOME, ActivityTypes.WORK, ActivityTypes.EDUCATION, ActivityTypes.LEISURE, ActivityTypes.SHOPPING, ActivityTypes.OTHER};

        SplitActivityTypesBasedOnDuration splitAct = new SplitActivityTypesBasedOnDuration(inputPopulationFile);
        splitAct.run(outputPopulationFile, outputConfigFile, timeBinSize_s, activities, 3600.0*3.0);
    }

}
