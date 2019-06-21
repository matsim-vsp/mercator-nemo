package org.matsim.nemo;


import java.nio.file.Path;
import java.nio.file.Paths;

class SupplyOutput {

	private static final String OUTPUT_SUPPLY_DIR = "/projects/nemo_mercator/data/matsim_input/supply";

	private String svnDir;

	SupplyOutput(String svnDir) {
		this.svnDir = svnDir;
	}

	Path getOutputNetworkDir() {
		return Paths.get(svnDir, OUTPUT_SUPPLY_DIR);
	}

}
