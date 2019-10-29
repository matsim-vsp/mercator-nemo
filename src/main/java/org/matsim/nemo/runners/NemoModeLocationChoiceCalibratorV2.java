package org.matsim.nemo.runners;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.nemo.RuhrAgentsFilter;
import org.matsim.nemo.util.NEMOUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.DistanceDistribution;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.planselectors.InitialPlanKeeperPlanRemoval;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;

public class NemoModeLocationChoiceCalibratorV2 {

    private final String inputDir;
    private final String runId;
    private final String outputDirectory;
    private final double cadytsCountsWeight;
    private final double cadytsMarginalsWeight;
    private Config config;
    private Scenario scenario;
    private Controler controler;

    private NemoModeLocationChoiceCalibratorV2(LocationChoiceArguments arguments) {

        if (arguments.countsWeight <= 0 || arguments.marginalsWeight <= 0)
            throw new RuntimeException("counts weight and marginals weight must be greater 0. Otherwise we get unspecified behavior");

        this.inputDir = arguments.inputDir;
        this.runId = arguments.runId;
        this.outputDirectory = arguments.outputDir;
        this.cadytsCountsWeight = arguments.countsWeight;
        this.cadytsMarginalsWeight = arguments.marginalsWeight;
    }

    public static void main(String[] args) {

        LocationChoiceArguments arguments = new LocationChoiceArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        new NemoModeLocationChoiceCalibratorV2(arguments).run();
    }

    public void run() {

        if (controler == null) controler = prepareControler();
        controler.run();
    }

    private Controler prepareControler() {

        if (scenario == null) scenario = prepareScenario();

        Controler controler = new Controler(scenario);


        //keep initial plans
        controler.getConfig().strategy().setPlanSelectorForRemoval(InitialPlanKeeperPlanRemoval.initial_plans_keeper_plan_remover);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // what is this doing?
                if (getConfig().strategy().getPlanSelectorForRemoval().equals(InitialPlanKeeperPlanRemoval.initial_plans_keeper_plan_remover))
                    bindPlanSelectorForRemoval().to(InitialPlanKeeperPlanRemoval.class);
            }
        });

        // use car-travel time calculator for ride mode to teleport them yet affected by congestion.
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        // marginal cadyts
        DistanceDistribution distanceDistribution = createDistanceDistribution(0.01);
        RuhrAgentsFilter filter = new RuhrAgentsFilter(scenario, inputDir + "/ruhrgebiet_boundary.shp");
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(AgentFilter.class).toInstance(filter);
                bind(DistanceDistribution.class).toInstance(distanceDistribution);
            }
        });
        controler.addOverridingModule(new ModalDistanceCadytsModule());

        // counts cadyts
        controler.addOverridingModule(new CadytsCarModule());

        final double cadytsCountsScoringWeight = cadytsCountsWeight * config.planCalcScore().getBrainExpBeta();
        final double cadytsMarginalsScoringWeight = cadytsMarginalsWeight * config.planCalcScore().getBrainExpBeta();

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            ScoringParametersForPerson parameters;
            @Inject
            private CadytsContext cadytsContext;
            @Inject
            private ModalDistanceCadytsContext marginalCadytsContext;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                final ScoringParameters params = parameters.getScoringParameters(person);
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                        controler.getScenario().getNetwork(), new HashSet<>(Collections.singletonList("pt"))));
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<>(person.getSelectedPlan(),
                        config,
                        cadytsContext);
                scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);
                sumScoringFunction.addScoringFunction(scoringFunctionCounts);


                final CadytsScoring<Id<DistanceDistribution.DistanceBin>> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                        config,
                        marginalCadytsContext);

                scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
                sumScoringFunction.addScoringFunction(scoringFunctionMarginals);
                return sumScoringFunction;
            }
        });
        return controler;
    }

    private Scenario prepareScenario() {

        if (config == null) config = prepareConfig();

        Scenario result = ScenarioUtils.loadScenario(config);

        // add stay home plan if it does not exists
        for (Person person : result.getPopulation().getPersons().values()) {
            if (person.getPlans().stream().noneMatch(pl -> pl.getPlanElements().size() == 1)) {

                Plan stayHome = result.getPopulation().getFactory().createPlan();
                Activity existAct = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                Activity activity = result.getPopulation().getFactory().createActivityFromCoord("home_86400.0", existAct.getCoord());
                activity.setLinkId(existAct.getLinkId());
                stayHome.addActivity(activity);
                AttributesUtils.copyAttributesFromTo(existAct, activity);
                person.addPlan(stayHome);
            }
        }
        return result;
    }

    private Config prepareConfig() {

        Config result = ConfigUtils.loadConfig(inputDir + "/config.xml");
        result.controler().setRunId(runId);
        result.controler().setOutputDirectory(outputDirectory);
        result.plansCalcRoute().setInsertingAccessEgressWalk(true);
        result.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);

        result.qsim().setUsingTravelTimeCheckInTeleportation(true);
        result.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
        result.strategy().setMaxAgentPlanMemorySize(15);

        //   result.facilities().setAddEmptyActivityOption(false);
        //   result.facilities().setRemovingLinksAndCoordinates(false); //keeping coordinates
        //   result.facilities().setAssigningOpeningTime(false);
        result.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile);

        final long minDuration = 600;
        final long maxDuration = 3600 * 27;
        final long difference = 600;
        NEMOUtils.createTypicalDurations("home", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));
        NEMOUtils.createTypicalDurations("work", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));
        NEMOUtils.createTypicalDurations("education", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));
        NEMOUtils.createTypicalDurations("leisure", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));
        NEMOUtils.createTypicalDurations("shopping", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));
        NEMOUtils.createTypicalDurations("other", minDuration, maxDuration, difference).forEach(params -> result.planCalcScore().addActivityParams(params));

        return result;
    }

    private DistanceDistribution createDistanceDistribution(double scalingFactor) {

        DistanceDistribution distanceDistribution = new DistanceDistribution(scalingFactor);
        distanceDistribution.add(TransportMode.car, 0, 1000, 10000, 357488);
        distanceDistribution.add(TransportMode.pt, 0, 1000, 10000, 28540);
        distanceDistribution.add(TransportMode.bike, 0.0, 1000, 10000, 212908);
        distanceDistribution.add(TransportMode.walk, 0.0, 1000, 10000, 1842358);
        distanceDistribution.add(TransportMode.ride, 0.0, 1000, 1000, 124379);

        distanceDistribution.add(TransportMode.car, 1000.0, 3000, 10000, 1213004);
        distanceDistribution.add(TransportMode.pt, 1000.0, 3000, 10000, 437762);
        distanceDistribution.add(TransportMode.bike, 1000.0, 3000, 10000, 541548);
        distanceDistribution.add(TransportMode.walk, 1000.0, 3000, 10000, 832557);
        distanceDistribution.add(TransportMode.ride, 1000.0, 3000, 1000, 483180);

        distanceDistribution.add(TransportMode.car, 3000.0, 5000, 10000, 842537);
        distanceDistribution.add(TransportMode.pt, 3000.0, 5000, 10000, 323085);
        distanceDistribution.add(TransportMode.bike, 3000.0, 5000, 10000, 328707);
        distanceDistribution.add(TransportMode.walk, 3000.0, 5000, 10000, 132547);
        distanceDistribution.add(TransportMode.ride, 3000.0, 5000, 1000, 232588);

        distanceDistribution.add(TransportMode.car, 5000.0, 10000, 10000, 1274829);
        distanceDistribution.add(TransportMode.pt, 5000.0, 10000, 10000, 326567);
        distanceDistribution.add(TransportMode.bike, 5000.0, 10000, 10000, 99804);
        distanceDistribution.add(TransportMode.walk, 5000.0, 10000, 10000, 68559);
        distanceDistribution.add(TransportMode.ride, 5000.0, 10000, 1000, 325927);

        distanceDistribution.add(TransportMode.car, 10000.0, 1000000, 10000, 1979366);
        distanceDistribution.add(TransportMode.pt, 10000.0, 1000000, 10000, 456848);
        distanceDistribution.add(TransportMode.bike, 10000.0, 1000000, 10000, 63259);
        distanceDistribution.add(TransportMode.walk, 10000.0, 1000000, 10000, 33138);
        distanceDistribution.add(TransportMode.ride, 10000.0, 1000000, 1000, 322899);

        return distanceDistribution;
    }

    private static class LocationChoiceArguments {

        @Parameter(names = "-inputDir", required = true)
        String inputDir;

        @Parameter(names = "-outputDir", required = true)
        String outputDir;

        @Parameter(names = "-runId", required = true)
        String runId;

        @Parameter(names = "-countsWeight")
        double countsWeight = 15;

        @Parameter(names = "-marginalsWeight")
        double marginalsWeight = 5.0;
    }
}
