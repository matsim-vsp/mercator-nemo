package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.nemo.RuhrAgentsFilter;

import java.nio.file.Paths;

public class PlansAnalysis {

    @Parameter(names = "-pf", required = true)
    private String plansFile = "";

    @Parameter(names = "-sf", required = true)
    private String shapeFile = "";

    public static void main(String[] args) {

        PlansAnalysis plansAnalysis = new PlansAnalysis();
        JCommander.newBuilder().addObject(plansAnalysis).build().parse(args);

        plansAnalysis.run();
    }

    private void run() {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        new PopulationReader(scenario).readFile(Paths.get(plansFile).toString());
        var filter = new RuhrAgentsFilter(scenario, ShapeFileReader.getAllFeatures(Paths.get(shapeFile).toString()));


        long numberOfLegs = scenario.getPopulation().getPersons().values().parallelStream()
                .filter(person -> filter.includeAgent(person.getId()))
                .map(HasPlansAndId::getSelectedPlan)
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Activity)
                .map(element -> (Activity) element)
                .skip(1)
                .filter(activity -> !StageActivityTypeIdentifier.isStageActivity(activity.getType()))
                .count();

        long numberOfLegsFromTripStructureUtils = scenario.getPopulation().getPersons().values().parallelStream()
                .filter(person -> filter.includeAgent(person.getId()))
                .map(HasPlansAndId::getSelectedPlan)
                .mapToLong(plan -> TripStructureUtils.getTrips(plan).size())
                .sum();

        System.out.println(numberOfLegs);
        System.out.println(numberOfLegsFromTripStructureUtils);
    }
}
