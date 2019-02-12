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

import com.google.inject.Key;
import com.google.inject.name.Names;
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
import org.matsim.core.scoring.functions.*;
import org.matsim.scenarioCalibration.marginals.RuhrAgentsFilter;
import org.matsim.util.NEMOUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.ModalDistanceAnalysisModule;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;
import playground.vsp.planselectors.InitialPlanKeeperPlanRemoval;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
            // the following comments show the settings for cadyts run 307
            configFile = args[0]; ///net/ils4/ziemke/nemo/marginals/input/preparedConfig_25.xml
            outputDir = args[1]; ///net/ils4/ziemke/nemo/marginals/$JOB_NAME/output/
            runId = args[2]; //$JOB_NAME
            lastIt = Integer.valueOf(args[3]); //700
            cadytsCountsWt = Double.valueOf(args[4]); //15.0
            cadytsMarginalsWt = Double.valueOf(args[5]); //5.0
            shapeFile = args[6]; // /net/ils4/agarwal/nemo/data/marginals/input/ruhrgebiet_boundary.shp
            keepInitialPlans = Boolean.valueOf(args[7]); //true

            if (args.length > 8) removeStayHomePlanForMaxShortDistTrips = Boolean.valueOf(args[8]); //false
            if (args.length > 9) mergeShortDistanceBins = Boolean.valueOf(args[9]); // true
            if (args.length > 10) useEssenBochumReferenceData = Boolean.valueOf(args[10]); //false
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
            if (person.getPlans().stream().anyMatch(pl -> pl.getPlanElements().size() == 1)) {
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
                            controler.getScenario().getNetwork(), new HashSet<>(Collections.singletonList("pt"))));
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
                            controler.getScenario().getNetwork(), new HashSet<>(Collections.singletonList("pt"))));
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
                            controler.getScenario().getNetwork(), new HashSet<>(Collections.singletonList("pt"))));
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
        controler.addOverridingModule(NEMOUtils.createModalShareAnalysis());
        controler.run();
    }
}
