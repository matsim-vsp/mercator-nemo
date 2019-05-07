package org.matsim.scenarioCreation.network;


import java.nio.file.Path;
import java.nio.file.Paths;

public class SupplyOutput {

    private static final String OUTPUT_SUPPLY_DIR = "/projects/nemo_mercator/data/matsim_input/supply";

    private String svnDir;

    public SupplyOutput(String svnDir) {
        this.svnDir = svnDir;
    }

    public Path getOutputNetworkDir() {
        return Paths.get(svnDir, OUTPUT_SUPPLY_DIR);
    }

}
