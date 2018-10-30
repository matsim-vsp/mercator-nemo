package org.matsim.scenarioCreation.network;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NetworkOutput {

    private static final String OUTPUT_NETWORK_DIR = "/projets/nemo_mercartor/data/matsim_input/network";
    private static final String OUTPUT_COUNTS_DIR = "/projects/nemo_mercartor/data/matsim_input/counts/car_counts.xml";

    private String svnDir;

    public String getOutputNetworkDir() {
        return svnDir + OUTPUT_NETWORK_DIR;
    }

    String getOutputCountsDir() {
        return svnDir + OUTPUT_COUNTS_DIR;
    }

}
