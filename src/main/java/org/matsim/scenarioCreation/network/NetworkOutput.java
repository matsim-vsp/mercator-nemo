package org.matsim.scenarioCreation.network;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NetworkOutput {

    private static final String OUTPUT_DIR = "/projets/nemo_mercartor/data/matsim_input/network";
    private static final String OUTPUT_COUNTS = "/projects/nemo_mercartor/data/matsim_input/counts/car_counts.xml";

    private String svnDir;

    public String getOutputDir() {
        return svnDir + OUTPUT_DIR;
    }

    public String getOutputCounts() {
        return svnDir + OUTPUT_COUNTS;
    }

}
