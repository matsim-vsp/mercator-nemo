package org.matsim.scenarioCalibration.marginals;

import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationUtils;
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
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.ModalDistanceDistributionControlerListener;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

public class ReRunningWithStayHomePlan {
	
	
	public static void main(String[] args) {
		
		
		String dir = "../../repos/runs-svn/nemo/marginals/run249/output/";
		String outputDirectory = "../../repos/runs-svn/nemo/marginals/run255/onlyPlanSelection/";
		String runNr = "run249";
		
		Config config = ConfigUtils.loadConfig(dir+runNr+".output_config.xml");
		config.plans().setInputFile(runNr+".output_plans.xml.gz");
		config.plans().setInputPersonAttributeFile(runNr+".output_personAttributes.xml.gz");
		
		config.network().setInputFile(runNr+".output_network.xml.gz");
		config.counts().setInputFile(runNr+".output_counts.xml.gz");
		
		config.vehicles().setVehiclesFile(runNr+".output_vehicles.xml.gz");
		
		config.strategy().clearStrategySettings();
		
		StrategySettings stratSets = new StrategySettings();
		stratSets.setWeight(1.0);
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		
		config.strategy().addStrategySettings(stratSets);
		config.strategy().setMaxAgentPlanMemorySize(12);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runNr+"_sel");
		
		int iteration =20;
		double cadytsCountsWt = 15.0;
        double cadytsMarginalsWt = 0.0;
        
        config.controler().setFirstIteration(config.controler().getLastIteration());
        config.controler().setLastIteration(config.controler().getLastIteration()+iteration);

        if (args.length == 0) {
            config.controler()
                  .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("ride").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });
        
        DistanceDistribution inputDistanceDistribution = NemoModeLocationChoiceCalibrator.getDistanceDistribution(config.counts().getCountsScaleFactor());
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
        
        controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(new IterationEndsListener() {
					
					@Inject private Population population;
					
					@Override
					public void notifyIterationEnds(IterationEndsEvent arg0) {
						for (Person person : population.getPersons().values()) {
						
							Activity existAct = ((Activity) person.getSelectedPlan().getPlanElements().get(0));
							Activity homeAct = population.getFactory().createActivityFromCoord("home_86400.0", existAct.getCoord());
							homeAct.setLinkId(existAct.getLinkId());
//							homeAct.setEndTime(existAct.getEndTime());

							Plan newPlan = population.getFactory().createPlan();
							newPlan.setScore( 60.0 );
							newPlan.addActivity(homeAct);
							
							person.addPlan(newPlan);
						}
					}
				});
				
			}
		});
        
        controler.run();
	}
	
	

}
