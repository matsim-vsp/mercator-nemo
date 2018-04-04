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

package org.matsim.scenarioCalibration.marginals;

import playground.agarwalamit.parametricRuns.PrepareParametricRuns;

/**
 * Created by amit.
 */

public class ParametricRunsNEMOMarginals {

    public static void main(String[] args) {
        int runCounter= 207;

        String baseOutDir = "/net/ils4/agarwal/nemo/data/marginals/";
        String matsimDir = "r_3e0c4e8ef53486796e955116184279c4fc88522b_nemoMarginals";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        Integer [] lastIts = {300,400};
        double [] cadytsCountsWts = {15};
        double [] cadytsMarginalsWts = {25,50};

        buffer.append("run201 to run206 had util_trav as -6.0. Setting it to zero in subsequent runs.");
        buffer.append("jobName\tconfigFile\toutputDir\tjobName\tlastIteration\tcadytsCountsWt\tcadytsMarginalsWt"+ PrepareParametricRuns.newLine);

        for (int lastIt : lastIts) {
            for (double countsCadytsWt : cadytsCountsWts ) {
                for(double cadytsMarginalsWt :cadytsMarginalsWts){

                        String configFile = "/net/ils4/agarwal/nemo/data/marginals/input/preparedConfig_rideAsMainMode.xml";
                        String jobName = "run"+String.valueOf(runCounter++);
                        String outputDir = baseOutDir+"/"+jobName+"/output/";

                        String params = configFile + " " + outputDir + " " + jobName + " "+lastIt + " " + countsCadytsWt + " "+ cadytsMarginalsWt ;

                        String [] additionalLines = {
                                "echo \"========================\"",
                                "echo \" "+matsimDir+" \" ",
                                "echo \"========================\"",
                                PrepareParametricRuns.newLine,

                                "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                PrepareParametricRuns.newLine,

                                "java -Djava.awt.headless=true -Xmx58G -cp nemo-0.10.0-SNAPSHOT.jar " +
                                        "org/matsim/scenarioCalibration/marginals/NemoModeLocationChoiceCalibrator " +
                                        params+" "
                        };

                        parametricRuns.appendJobParameters("-l mem_free=15G");// 4 cores with 15G each
                        parametricRuns.run(additionalLines, baseOutDir, jobName);

                        buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo_marginals.txt");
        parametricRuns.close();
    }
}
