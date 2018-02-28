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

import playground.agarwalamit.parametricRuns.PrepareParametricRuns;

/**
 * Created by amit on 15.02.18.
 */

public class ParametricRunsRuhrModeChoice {


    public static void main(String[] args) {
        int runCounter= 127;

        String baseOutDir = "/net/ils4/agarwal/nemo/data/modeChoice/";
        String matsimDir = "r_2d777f15e2d273864c526183f2d2e7f2eac6f2a9_nemoModeChoice";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        double [] bikeASCs = {0.0};
        double [] carASCs = { -2.5};
        double [] ptASCs = {-3.0,-3.25,-3.5};
        double [] bikeMargUtilDists = {-0.0005, 0.};
        double [] walkMargUtilDists = {0.0};

        buffer.append("jobName\tconfigFile\tjobName\toutputDir\tbikeASC\tcarASC\tptASC\tmarginalUtilDistBike\tmarginalUtilDistWalk"+ PrepareParametricRuns.newLine);

        for(double bikeASC : bikeASCs) {
            for (double carASC : carASCs ) {
                for(double ptASC :ptASCs){
                    for (double walkMargUtilDist : walkMargUtilDists) {
                        for (double bikeMargUtilDist : bikeMargUtilDists) {
                            String configFile = "/net/ils4/agarwal/nemo/data/modeChoice/input/config.xml";
                            String jobName = "run"+String.valueOf(runCounter++);
                            String outputDir = baseOutDir+"/"+jobName+"/output/";

                            String params = configFile + " " + jobName + " " + outputDir + " "+ bikeASC + " " + carASC + " " + ptASC + " "+ bikeMargUtilDist +" " + walkMargUtilDist;

                            String [] additionalLines = {
                                    "echo \"========================\"",
                                    "echo \" "+matsimDir+" \" ",
                                    "echo \"========================\"",
                                    PrepareParametricRuns.newLine,

                                    "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                    PrepareParametricRuns.newLine,

                                    "java -Djava.awt.headless=true -Xmx58G -cp nemo-0.10.0-SNAPSHOT.jar " +
                                            "org/matsim/scenarioCalibration/modeChoice/RuhrModeChoiceCalibrationControler " +
                                            params+" "
                            };

                            parametricRuns.appendJobParameters("-l mem_free=15G");// 4 cores with 15G each
                            parametricRuns.run(additionalLines, baseOutDir, jobName);

                            buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                        }
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo_modeChoice.txt");
        parametricRuns.close();
    }

}
