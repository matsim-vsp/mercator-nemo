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
import org.matsim.scenarioCalibration.marginals.RuhrAgentsFilter;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.ModalDistanceDistributionControlerListener;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

/**
 * Created by amit on 01.05.18.
 */

public class RuhrCountsCadytsRun {

    public static void main(String[] args) {

        String configFile = "../../repos/runs-svn/nemo/marginals/input/preparedConfig_allCarsPlainMATSimRun.xml";
        String outDir = "../../repos/runs-svn/nemo/marginals/testCalib/";
        String runID = "allCars_3";

        double cadytsWt = 15;
        double storageCapFactor = 0.3;

        if(args.length>0){
            configFile = args[0] ;
            outDir = args[1];
            runID = args[2];
            cadytsWt = Double.valueOf(args[3]);
            storageCapFactor = Double.valueOf(args[4]);
        }

        Config config = ConfigUtils.loadConfig(configFile);
        config.controler().setRunId(runID);
        config.controler().setOutputDirectory(outDir);
        double flowCapFactor = NEMOUtils.round(1 * NEMOUtils.SAMPLE_SIZE / NEMOUtils.RUHR_CAR_SHARE,2); //0.021
        config.qsim().setFlowCapFactor(  flowCapFactor);
        config.qsim().setStorageCapFactor(storageCapFactor);

        double countScaleFactor = NEMOUtils.round(1/flowCapFactor,2);
        config.counts().setCountsScaleFactor(countScaleFactor);

        if (args.length == 0) {
            config.controler()
                  .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CadytsCarModule());

        DistanceDistribution inputDistanceDistribution = NEMOUtils.getDistanceDistribution(countScaleFactor);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(DistanceDistribution.class).toInstance(inputDistanceDistribution);
                this.bind(BeelineDistanceCollector.class);
                this.addControlerListenerBinding().to(ModalDistanceDistributionControlerListener.class);
                bind(AgentFilter.class).to(RuhrAgentsFilter.class);
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
}
