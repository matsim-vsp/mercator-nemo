package org.matsim.scenarioCreation.network;

import lombok.AllArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class NetworkOutput {

    private static final String OUTPUT_NETWORK_DIR = "/projects/nemo_mercator/data/matsim_input/network";

    private String svnDir;

    public Path getOutputNetworkDir() {
        return Paths.get(svnDir, OUTPUT_NETWORK_DIR);
    }

}
