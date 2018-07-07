package org.matsim.scenarioCalibration.marginals.controler;

import java.io.File;
import javax.inject.Inject;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.scenarioCalibration.marginals.stayHomePlan.StayHomeScoreUpdater;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.ModalDistanceDistributionControlerListener;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

public class ReRunningWithStayHomePlan {
	
	
	public static void main(String[] args) {
		
		String parentDir;
        String runIdForInputFiles;
		String runId;

		int firstIt = 240; // --> 300*0.8;
		int lastIt = 300;

		double stayHomePlanScore = 144.0; //beta * typDur

        double cadytsCountsWt = 15.0;
        double cadytsMarginalsWt = 0.0;

        boolean useUtilPerf4ScoreStayHomePlan = false;
        int preparatoryIterations = 50;

        boolean mergeShortDistanceBins = false;

		if (args.length>0) {

            parentDir = args[0];
            runIdForInputFiles = args[1];
            runId = args[2];
            stayHomePlanScore = Double.valueOf(args[3]);


            cadytsCountsWt = Double.valueOf(args[4]);
            cadytsMarginalsWt = Double.valueOf(args[5]);

            useUtilPerf4ScoreStayHomePlan = Boolean.valueOf(args[6]); // if false, args[3] doesn't matter.
            preparatoryIterations = Integer.valueOf(args[7]);

            if (args.length>8) mergeShortDistanceBins = Boolean.valueOf(args[8]);

        } else {
            parentDir = "../../repos/runs-svn/nemo/marginals/";
            runIdForInputFiles = "run249_SHP";

            runId = runIdForInputFiles+"_planSelection";
        }

        String configFile = parentDir+runIdForInputFiles+"/output/"+runIdForInputFiles+".output_config.xml";
		String plansFile = parentDir+runIdForInputFiles+"/output/ITERS/it."+firstIt+"/"+runIdForInputFiles+"."+firstIt+".plans.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);

		config.plans().setInputFile(new File(plansFile).getAbsolutePath());
		config.plans().setInputPersonAttributeFile(runIdForInputFiles+".output_personAttributes.xml.gz");
		
		config.network().setInputFile(runIdForInputFiles+".output_network.xml.gz");
		config.counts().setInputFile(runIdForInputFiles+".output_counts.xml.gz");
		
		config.vehicles().setVehiclesFile(runIdForInputFiles+".output_vehicles.xml.gz");
		
		config.strategy().clearStrategySettings(); // only plan selection
		
		StrategySettings stratSets = new StrategySettings();
		stratSets.setWeight(1.0);
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		
		config.strategy().addStrategySettings(stratSets);
		config.strategy().setMaxAgentPlanMemorySize(12);
		
		config.controler().setOutputDirectory(parentDir+runId+"/output/");
		config.controler().setRunId(runId);
		
        config.controler().setFirstIteration(firstIt);
        config.controler().setLastIteration(lastIt);

        if (args.length == 0) {
            config.controler()
                  .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        if ( ! useUtilPerf4ScoreStayHomePlan) {
            ConfigUtils.addOrGetModule(scenario.getConfig(), CadytsConfigGroup.class).setPreparatoryIterations(preparatoryIterations);
            //also update last iteration
            config.controler().setLastIteration(lastIt+preparatoryIterations);

            // remove the scores of all plans
            scenario.getPopulation().getPersons().values().stream().flatMap(p->p.getPlans().stream()).forEach(pl -> pl.setScore(null));
        }

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("ride").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });
        
        DistanceDistribution inputDistanceDistribution = NEMOUtils.getDistanceDistribution(config.counts().getCountsScaleFactor(), scenario.getConfig().plansCalcRoute(), mergeShortDistanceBins,
                true);
        if (cadytsMarginalsWt !=0.){
            controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));

        } else { //get the analysis at least
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    this.bind(DistanceDistribution.class).toInstance(inputDistanceDistribution);
                    this.bind(BeelineDistanceCollector.class);
                    this.addControlerListenerBinding().to(ModalDistanceDistributionControlerListener.class);
                }
            });
        }
        
        controler.addOverridingModule(new CadytsCarModule());
        final double cadytsCountsScoringWeight = cadytsCountsWt * config.planCalcScore().getBrainExpBeta();

        if (cadytsMarginalsWt!=0.) {
            final double cadytsMarginalsScoringWeight = cadytsMarginalsWt * config.planCalcScore().getBrainExpBeta();
            controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
                @Inject
                private CadytsContext cadytsContext;
                @Inject
                ScoringParametersForPerson parameters;

                @Inject private ModalDistanceCadytsContext marginalCadytsContext;

                @Override
                public ScoringFunction createNewScoringFunction(Person person) {
                    SumScoringFunction sumScoringFunction = new SumScoringFunction();

                    final ScoringParameters params = parameters.getScoringParameters(person);
                    sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                            controler.getScenario().getNetwork()));
                    sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                    sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                    final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
                            config,
                            cadytsContext);
                    scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);

                    final CadytsScoring<ModalDistanceBinIdentifier> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                                config,
                                marginalCadytsContext);

                    scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
                    sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

                    sumScoringFunction.addScoringFunction(scoringFunctionCounts);

                    return sumScoringFunction;
                }
            });
        } else {
            controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
                @Inject
                private CadytsContext cadytsContext;
                @Inject
                ScoringParametersForPerson parameters;

                @Override
                public ScoringFunction createNewScoringFunction(Person person) {
                    SumScoringFunction sumScoringFunction = new SumScoringFunction();

                    final ScoringParameters params = parameters.getScoringParameters(person);
                    sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                            controler.getScenario().getNetwork()));
                    sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                    sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                    final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
                            config,
                            cadytsContext);
                    scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);

                    sumScoringFunction.addScoringFunction(scoringFunctionCounts);

                    return sumScoringFunction;
                }
            });
        }

        //analyses
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        if (useUtilPerf4ScoreStayHomePlan ) {
            final double scoreToSet = stayHomePlanScore;
            controler.addOverridingModule(new AbstractModule() {

                @Override
                public void install() {
                    addControlerListenerBinding().toInstance(new IterationEndsListener() {

                        @Inject private Population population;

                        @Override
                        public void notifyIterationEnds(IterationEndsEvent event) {
                            for (Person person : population.getPersons().values()) {

                                Plan plan = getStayHomePlan(person);
                                if (plan==null){
                                    Activity existAct = ((Activity) person.getSelectedPlan().getPlanElements().get(0));
                                    Activity homeAct = population.getFactory().createActivityFromCoord("home_86400.0", existAct.getCoord());
                                    homeAct.setLinkId(existAct.getLinkId());
                                    existAct.getAttributes()
                                            .getAsMap()
                                            .forEach((key, value) -> homeAct.getAttributes().putAttribute(key, value));

                                    plan = population.getFactory().createPlan();
                                    plan.addActivity(homeAct);
                                    person.addPlan(plan);
                                }
                                plan.setScore(scoreToSet);
                            }
                        }
                    });
                }
            });
        } else {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addControlerListenerBinding().to(StayHomeScoreUpdater.class);
                }
            });
        }

        controler.run();
	}
	
	private static Plan getStayHomePlan (Person person) {
        for (Plan plan : person.getPlans()) {
            if (plan.getPlanElements().size() == 1) return plan;
        }
        return null;
    }

}
