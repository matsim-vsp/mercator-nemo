package org.matsim.scenarioCreation.network;


import java.nio.file.Path;
import java.nio.file.Paths;

class NetworkOutput {

    private static final String OUTPUT_NETWORK_DIR = "/projects/nemo_mercator/data/matsim_input/network";

    private String svnDir;

    NetworkOutput(String svnDir) {
        this.svnDir = svnDir;
    }

    Path getOutputNetworkDir() {
        return Paths.get(svnDir, OUTPUT_NETWORK_DIR);
    }

}
