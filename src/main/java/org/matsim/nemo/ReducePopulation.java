package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ReducePopulation {

    public static void main(String[] args) {

        Input input = new Input();
        JCommander.newBuilder().addObject(input).build().parse(args);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
        StreamingPopulationWriter writer = new StreamingPopulationWriter(0.1); // take all persons
        reader.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {

            }
        });

        try {
            writer.startStreaming(input.outputFile);
            reader.readFile(input.populationFile);
        } finally {
            writer.closeStreaming();
        }
    }

    private static class Input {

        @Parameter(names = "-populationFile")
        private String populationFile;

        @Parameter(names = "-outputFile")
        private String outputFile;
    }
}
