package org.matsim.scenarioCalibration.baseCase;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class BaseCaseWithBikeHighwaysRunner {

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        BaseCaseCalibrationRunner runner = new BaseCaseCalibrationRunner(arguments.runId, arguments.outputDir, arguments.inputDir);
        runner.run();
    }

    private static class InputArguments {

        @Parameter(names = "-inputDir", required = true)
        private String inputDir;

        @Parameter(names = "-outputDir", required = true)
        private String outputDir;

        @Parameter(names = "-runId", required = true)
        private String runId;
    }
}
