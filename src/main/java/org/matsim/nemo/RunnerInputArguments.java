package org.matsim.nemo;

import com.beust.jcommander.Parameter;

class RunnerInputArguments {

    @Parameter(names = "-configPath", required = true)
    private String configPath;

    @Parameter(names = "-runId", required = true)
    private String runId;

    @Parameter(names = "-outputDir", required = true)
    private String outputDir;

    @Parameter(names = "-inputDir", required = true)
    private String inputDir;

    String getConfigPath() {
        return configPath;
    }

    String getRunId() {
        return runId;
    }

    String getOutputDir() {
        return outputDir;
    }

    String getInputDir() {
        return inputDir;
    }
}
