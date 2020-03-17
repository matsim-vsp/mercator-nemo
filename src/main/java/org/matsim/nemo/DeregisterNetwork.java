package org.matsim.nemo;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class DeregisterNetwork {

    private static final String inputPopulation = "C:\\Users\\Janekdererste\\repos\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\healthy\\baseCase_021.output_plans.xml.gz";
    private static final String outputPopulation = "C:\\Users\\Janekdererste\\repos\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\healthy\\baseCase_021.output_plans.xml.gz";

    public static void main(String[] args) {

        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(inputPopulation);

        scenario.getPopulation().getPersons().values().parallelStream()
                .flatMap(person -> person.getPlans().stream())
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Activity)
                .map(element -> (Activity) element)
                .forEach(activity -> {
                    activity.setLinkId(null);
                    activity.setFacilityId(null);
                });

        scenario.getPopulation().getPersons().values().parallelStream()
                .flatMap(person -> person.getPlans().stream())
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Leg)
                .map(element -> (Leg) element)
                .forEach(leg -> leg.setRoute(null));

        new PopulationWriter(scenario.getPopulation()).write(outputPopulation);


    }
}
