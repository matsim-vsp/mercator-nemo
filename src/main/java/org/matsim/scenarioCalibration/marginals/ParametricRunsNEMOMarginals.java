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
        int runCounter= 201;

        String baseOutDir = "/net/ils4/agarwal/nemo/data/locationChoice/";
        String matsimDir = "r_1d0ceb502e399ba38762743485c1bb02a6aeedc0_nemo05Feb";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        Integer [] lastIts = {200};
        double [] cadytsCountsWts = {15};
        double [] cadytsMarginalsWts = {50,100,500};

        buffer.append("jobName\tconfigFile\tplansFile\tnetworkFile\tcountsFile\toutputDir\tjobName\tflowCapacityFactor\tstorageCapacityFactor\tlastIteration\tcadytsWt"+ PrepareParametricRuns.newLine);

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

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo_marginals.txt");
        parametricRuns.close();
    }
}
