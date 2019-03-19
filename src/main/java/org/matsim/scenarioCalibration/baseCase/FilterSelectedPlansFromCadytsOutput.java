package org.matsim.scenarioCalibration.baseCase;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.util.NEMOUtils;
import playground.vsp.planselectors.InitialPlanKeeperPlanRemoval;

public class FilterSelectedPlansFromCadytsOutput {

	private static final String plansFilePath = "/nemo/cadytsV2/cadytsV2_014/cadytsV2_014.output_plans.xml.gz";
	private static final String outputFilePath = "/projects/nemo_mercator/data/matsim_input/baseCase/selectedPlans_from_cadytsV2_014.xml.gz";

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
        Population inputPopulation = NEMOUtils.loadScenarioFromPlans(arguments.runsSvnPath + plansFilePath).getPopulation();

        inputPopulation.getPersons().values().forEach(person -> {
            Plan selectedPlan = clearLinkInfo(person.getSelectedPlan());
            selectedPlan.getAttributes().removeAttribute(InitialPlanKeeperPlanRemoval.plan_attribute_name);
            Person outPerson = outputPopulation.getFactory().createPerson(person.getId());
            outPerson.addPlan(selectedPlan);
            outputPopulation.addPerson(outPerson);
        });

        new PopulationWriter(outputPopulation).write(arguments.sharedSvnPath + outputFilePath);
    }

    private static Plan clearLinkInfo(Plan plan) {

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = ((Activity) planElement);
                if (activity.getLinkId() == null) continue;
                if (activity.getCoord() != null) activity.setLinkId(null);
                else if (activity.getFacilityId() != null)
                    throw new RuntimeException("Activity has facility id: " + planElement + " Get the coordinate from facility");
                else throw new RuntimeException(" Activity" + planElement + " has neither link nor facility id!");
            }
            if (planElement instanceof Leg) {
                ((Leg) planElement).setRoute(null);
            }
        }
        return plan;
    }

    private static class InputArguments {

        @Parameter(names = "-shared-svn", required = true)
        private String sharedSvnPath;

        @Parameter(names = "-runs-svn", required = true)
        private String runsSvnPath;
    }
}
