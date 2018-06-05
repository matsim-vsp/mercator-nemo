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

package org.matsim.scenarioCalibration.marginals.controler;

import playground.vsp.parametricRuns.PrepareParametricRuns;

/**
 * Created by amit.
 */

public class ParametricRunsNEMOMarginals {

    public static void main(String[] args) {
        int runCounter= 267;

        String baseOutDir = "/net/ils4/agarwal/nemo/data/marginals/";
        String matsimDir = "r_c713a936cbbd305e8a0c0ef7d1e1ee13d640a646_nemo_05June18";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        Integer [] lastIts = {300};
        double [] cadytsCountsWts = { 15, 0};
        double [] cadytsMarginalsWts = {25, 15, 10, 5, 0};

        buffer.append("jobName\tconfigFile\toutputDir\tjobName\tlastIteration\tcadytsCountsWt\tcadytsMarginalsWt"+ PrepareParametricRuns.newLine);

        for (int lastIt : lastIts) {
            for (double countsCadytsWt : cadytsCountsWts ) {
                for(double cadytsMarginalsWt :cadytsMarginalsWts){

                        String configFile = "/net/ils4/agarwal/nemo/data/marginals/input/preparedConfig.xml";
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

                                "java -Djava.awt.headless=true -Xmx58G -cp nemo-0.0.1-SNAPSHOT.jar " +
                                        "org/matsim/scenarioCalibration/marginals/controler/NemoModeLocationChoiceCalibrator " +
                                        params+" "+"/net/ils4/agarwal/nemo/data/marginals/input/ruhrgebiet_boundary.shp"
                        };

                        parametricRuns.appendJobParameters("-l mem_free=15G");// 4 cores with 7.5G each
                        parametricRuns.run(additionalLines, baseOutDir, jobName);

                        buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo_marginals.txt");
        parametricRuns.close();
    }
}
