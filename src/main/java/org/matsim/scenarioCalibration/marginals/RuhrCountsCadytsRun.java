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

package org.matsim.scenarioCalibration.marginals;

import javax.inject.Inject;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.ModalDistanceDistributionControlerListener;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

/**
 * Created by amit on 01.05.18.
 */

public class RuhrCountsCadytsRun {

    public static void main(String[] args) {

        String configFile = "../../repos/runs-svn/nemo/marginals/input/preparedConfig_allCarsPlainMATSimRun.xml";
        String outDir = "../../repos/runs-svn/nemo/marginals/testCalib/";
        String runID = "run000";

        double cadytsWt = 15;

        if(args.length>0){
            configFile = args[0] ;
            outDir = args[1];
            runID = args[2];
            cadytsWt = Double.valueOf(args[3]);
        }

        Config config = ConfigUtils.loadConfig(configFile);
        config.controler().setRunId(runID);
        config.controler().setOutputDirectory(outDir);

        if (args.length == 0) {
            config.controler()
                  .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CadytsCarModule());

        DistanceDistribution inputDistanceDistribution = getDistanceDistribution();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(DistanceDistribution.class).toInstance(inputDistanceDistribution);
                this.bind(BeelineDistanceCollector.class);
                this.addControlerListenerBinding().to(ModalDistanceDistributionControlerListener.class);
            }
        });

        final double cadytsScoringWeight = cadytsWt * config.planCalcScore().getBrainExpBeta();
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

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(),
                        config,
                        cadytsContext);
                scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                sumScoringFunction.addScoringFunction(scoringFunction);

                return sumScoringFunction;
            }
        });
        controler.run();
    }

    private static DistanceDistribution getDistanceDistribution(){
        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();

        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("car",1.3); //+pt

        inputDistanceDistribution.setModeToScalingFactor("car", (1 / NEMOUtils.SAMPLE_SIZE) * NEMOUtils.RUHR_CAR_SHARE / (1) ); // -> (carShare + pt Shapre ) * 100 / carShare

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(0.0,1000.),1745861.0); //car+PT
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(1000.0,3000.),2733563.0);
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(3000.0,5000.),2026614.0);
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(5000.0,10000.),3103984.0);
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(10000.0,1000000.),2852334);
        return inputDistanceDistribution;
    }

}
