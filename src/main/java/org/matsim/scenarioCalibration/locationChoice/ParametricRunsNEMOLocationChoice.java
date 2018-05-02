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

import playground.vsp.parametricRuns.PrepareParametricRuns;

/**
 * Created by amit.
 */

public class ParametricRunsNEMOLocationChoice {

    public static void main(String[] args) {
        int runCounter= 8;

        String baseOutDir = "/net/ils4/agarwal/nemo/data/locationChoice/";
        String matsimDir = "r_1d0ceb502e399ba38762743485c1bb02a6aeedc0_nemo05Feb";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        double [] flowCaps = {0.02, 0.025};
        double [] storageCaps = {0.03};
        double [] cadytsWts = {0.15};
        Integer [] lastIts = {200};

        buffer.append("jobName\tconfigFile\tplansFile\tnetworkFile\tcountsFile\toutputDir\tjobName\tflowCapacityFactor\tstorageCapacityFactor\tlastIteration\tcadytsWt"+ PrepareParametricRuns.newLine);

        for (double flowCap : flowCaps ) {
            for(double storageCap :storageCaps){
                for (double cadytsWt : cadytsWts) {
                    for (int lastIt : lastIts) {

                        if (storageCap<flowCap) continue;

                        String configFile = "/net/ils4/agarwal/nemo/data/locationChoice/input/config.xml";
                        String plansFile = "/net/ils4/agarwal/nemo/data/input/matsim_initial_plans/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";
                        String networkFile = "/net/ils4/agarwal/nemo/data/input/network/allWaysNRW/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz";
                        String countsFile = "/net/ils4/agarwal/nemo/data/input/counts/24112017/NemoCounts_data_allCounts_Pkw.xml";
                        String jobName = "run"+String.valueOf(runCounter++);
                        String outputDir = baseOutDir+"/"+jobName+"/output/";

                        String params = configFile + " "+ plansFile + " " + networkFile + " " +countsFile+ " " + outputDir + " " + jobName + " "+flowCap + " " + storageCap + " "+ lastIt +" " +cadytsWt;

                        String [] additionalLines = {
                                "echo \"========================\"",
                                "echo \" "+matsimDir+" \" ",
                                "echo \"========================\"",
                                PrepareParametricRuns.newLine,

                                "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                PrepareParametricRuns.newLine,

                                "java -Djava.awt.headless=true -Xmx58G -cp nemo-0.10.0-SNAPSHOT.jar " +
                                        "org/matsim/scenarioCalibration/locationChoice/NemoLocationChoiceCalibration " +
                                        params+" "
                        };

                        parametricRuns.appendJobParameters("-l mem_free=15G");// 4 cores with 15G each
                        parametricRuns.run(additionalLines, baseOutDir, jobName);

                        buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
    }

}
