/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.scenarioCalibration.marginals.controler;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import javax.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.NEMOUtils;
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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.scenarioCalibration.marginals.RuhrAgentsFilter;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.ModalDistanceAnalysisModule;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;
import playground.vsp.planselectors.InitialPlanKeeperPlanRemoval;

/**
 * Created by amit on 01.03.18.
 */

public class NemoModeLocationChoiceCalibrator {

    public static void main(String[] args) {

        String configFile = "../../repos/shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/preparedConfig.xml";

        String outputDir = "../../repos/runs-svn/nemo/marginals/output/testCalib/";

        String runId = "testCalib";

        int lastIt = 1;
        double cadytsCountsWt =15.0;
        double cadytsMarginalsWt = 10.0;

        String shapeFile = NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE;

        boolean keepInitialPlans = true;
        boolean removeStayHomePlanForMaxShortDistTrips = true;

        boolean mergeShortDistanceBins = true;
        boolean useEssenBochumReferenceData = false;

        if (args.length > 0) {
            configFile = args[0];
            outputDir = args[1];
            runId = args[2];
            lastIt = Integer.valueOf(args[3]);
            cadytsCountsWt = Double.valueOf(args[4]);
            cadytsMarginalsWt = Double.valueOf(args[5]);
            shapeFile = args[6];
            keepInitialPlans = Boolean.valueOf(args[7]);

            if (args.length>8) removeStayHomePlanForMaxShortDistTrips = Boolean.valueOf(args[8]);
            if (args.length>9) mergeShortDistanceBins = Boolean.valueOf(args[9]);
            if (args.length>10) useEssenBochumReferenceData = Boolean.valueOf(args[10]);
        }

        Config config = ConfigUtils.loadConfig(configFile);

        config.controler().setOutputDirectory(new File(outputDir).getAbsolutePath());
        config.controler().setRunId(runId);
        config.controler().setLastIteration(lastIt);

        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
        config.strategy().setMaxAgentPlanMemorySize(15);

        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

        config.facilities().setAddEmptyActivityOption(false);
        config.facilities().setRemovingLinksAndCoordinates(false); //keeping coordinates
        config.facilities().setAssigningOpeningTime(false);
        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile);

        if (useEssenBochumReferenceData) {
            config.counts().setCountsScaleFactor(75.79);
        } else {
            config.counts().setCountsScaleFactor(74.386);
        }

        if (args.length == 0) {
            config.controler()
                  .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
            config.plans().setInputFile("/Users/amit/Documents/repos/runs-svn/nemo/marginals/input/sampledPop.xml");
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // network links --> allow car,ride
        scenario.getNetwork()
                .getLinks()
                .values()
                .forEach(l -> l.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.ride))));


        // add stay home plan if it does not exists
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (person.getPlans().stream().anyMatch(pl -> ((Plan) pl).getPlanElements().size()==1)) {
                // do nothing
            } else {
                Plan stayHome = scenario.getPopulation().getFactory().createPlan();
                Activity existAct = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("home_86400.0", existAct.getCoord());
                activity.setLinkId(existAct.getLinkId());
                stayHome.addActivity(activity);
                AttributesUtils.copyAttributesFromTo(existAct, activity);
                person.addPlan(stayHome);
            }

            // following is used for the cases when some agents has only one plan which has maximum number of short distance trips.
            if (! removeStayHomePlanForMaxShortDistTrips) continue;

            if (person.getPlans().size()==2) {
                Plan planForRemoval =null;
                for (Plan plan : person.getPlans()) {
                    if ( plan.getPlanElements().size()==1) {
                        planForRemoval = plan;
                    }
                }
                person.removePlan(planForRemoval);
            }
        }

        Controler controler = new Controler(scenario);

        if (keepInitialPlans) {
            controler.getConfig().strategy().setPlanSelectorForRemoval(InitialPlanKeeperPlanRemoval.initial_plans_keeper_plan_remover);
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    if (getConfig().strategy().getPlanSelectorForRemoval().equals(InitialPlanKeeperPlanRemoval.initial_plans_keeper_plan_remover)) {
                        bindPlanSelectorForRemoval().to(InitialPlanKeeperPlanRemoval.class);
                    }
                }
            });
        }

        // use car-travel time calculator for ride mode to teleport them yet affected by congestion.
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("ride").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });

        // marginals cadyts
        final String shapeFile_final = shapeFile;
        DistanceDistribution inputDistanceDistribution = NEMOUtils.getDistanceDistribution(config.counts()
                                                                                                 .getCountsScaleFactor(),
                scenario.getConfig().plansCalcRoute(),
                mergeShortDistanceBins, useEssenBochumReferenceData);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(AgentFilter.class).to(RuhrAgentsFilter.class);
                bind(Key.get(String.class, Names.named(RuhrAgentsFilter.ruhr_boundary_shape))).toInstance(shapeFile_final);
            }
        });

        if (cadytsMarginalsWt !=0.){
            controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));
        } else { //get the analysis at least
            controler.addOverridingModule(new ModalDistanceAnalysisModule(inputDistanceDistribution));
        }

        // counts cadyts
        if (cadytsCountsWt!=0.)  controler.addOverridingModule(new CadytsCarModule());

        final double cadytsCountsScoringWeight = cadytsCountsWt * config.planCalcScore().getBrainExpBeta();
        final double cadytsMarginalsScoringWeight = cadytsMarginalsWt * config.planCalcScore().getBrainExpBeta();

        if (cadytsCountsWt!=0. && cadytsMarginalsScoringWeight !=0.){ // we dont want to inject cadyts context if one of the two cadyts is off
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
                    sumScoringFunction.addScoringFunction(scoringFunctionCounts);


                    final CadytsScoring<ModalDistanceBinIdentifier> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                            config,
                            marginalCadytsContext);

                    scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
                    sumScoringFunction.addScoringFunction(scoringFunctionMarginals);
                    return sumScoringFunction;
                }
            });
        } else  if ( cadytsCountsWt!=0. ){ // we dont want to inject cadyts context if one of the two cadyts is off
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
        } else if ( cadytsMarginalsScoringWeight !=0.){ // we dont want to inject cadyts context if one of the two cadyts is off
            controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
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

                    final CadytsScoring<ModalDistanceBinIdentifier> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                            config,
                            marginalCadytsContext);

                    scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
                    sumScoringFunction.addScoringFunction(scoringFunctionMarginals);
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
        controler.run();
    }
}
