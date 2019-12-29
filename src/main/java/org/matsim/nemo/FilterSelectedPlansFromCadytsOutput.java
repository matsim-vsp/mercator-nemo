package org.matsim.nemo;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.nemo.util.NEMOUtils;
import playground.vsp.planselectors.InitialPlanKeeperPlanRemoval;

public class FilterSelectedPlansFromCadytsOutput {

    private static final String plansFilePath = "\\nemo\\cadytsV3\\cadytsV3_014\\output\\cadytsV3_014.output_plans.xml.gz";
    private static final String outputFilePath = "/projects/nemo_mercator/data/matsim_input/baseCase/selected-plans-from-cadytsV3_014.xml.gz";

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        Population inputPopulation = NEMOUtils.loadScenarioFromPlans(arguments.runsSvnPath + plansFilePath).getPopulation();

        inputPopulation.getPersons().values().parallelStream()
                .forEach(person -> {
                    Plan selectedPlan = person.getSelectedPlan();
                    selectedPlan.getAttributes().removeAttribute(InitialPlanKeeperPlanRemoval.plan_attribute_name);
                    person.getPlans().clear();
                    person.addPlan(selectedPlan);
                    person.setSelectedPlan(selectedPlan);

                    selectedPlan.getPlanElements().stream()
                            .filter(element -> element instanceof Leg)
                            .map(element -> (Leg) element)
                            .forEach(leg -> leg.setRoute(null));
                });
        new PopulationWriter(inputPopulation).write(arguments.sharedSvnPath + outputFilePath);
    }

    private static class InputArguments {

        @Parameter(names = "-sharedSvn", required = true)
        private String sharedSvnPath;

        @Parameter(names = "-runsSvn", required = true)
        private String runsSvnPath;
    }
}
