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

package org.matsim.scenarioCalibration.locationChoice;

/**
 * Created by amit on 09.11.17.
 */

public class PlansAnalyzer {

    public static void main(String[] args) {

        String initialPlansFile = "data/input/plans/2018_jan_22/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";
        String initialConfig = "data/locationChoice/input/config.xml";
        String outputFile = "data/input/plans/2018_jan_22/";
        //commenting following for time being, if required, move to vsp. Amit May'18
//        new OutputPlansConsistencyCheck(initialPlansFile, initialConfig, outputFile).run();

    }

}
