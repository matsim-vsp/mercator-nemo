package org.matsim.scenarioCreation.network;


import java.nio.file.Path;
import java.nio.file.Paths;

public class NetworkOutput {

    private static final String OUTPUT_NETWORK_DIR = "/projects/nemo_mercator/data/matsim_input/network";

    private String svnDir;

    public NetworkOutput(String svnDir) {
        this.svnDir = svnDir;
    }

    public Path getOutputNetworkDir() {
        return Paths.get(svnDir, OUTPUT_NETWORK_DIR);
    }

}
