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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.util.NEMOUtils;
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

import java.io.BufferedWriter;
import java.util.Map.Entry;

/**
 * Created by amit on 28.05.18.
 */

public class MarginalsOfflineAnalyser {

    public static void main(String[] args) {
        String dir = "../../repos/runs-svn/nemo/marginals/";
        String runCases [] = {"run269_b_ch1_maxShortTrips"};
//        String runCases [] = {"run000", "run249","run250","run251","run252","run253","run254","run255","run256","run257","run258","run259"};
        for (String runCase : runCases ){

            run(new String [] {dir, runCase, "0", "false"});
            run(new String [] {dir, runCase, "300", "false"});
        }
    }

    public static void run (String[] args) {

        String dir = args[0];
        String runId = args[1];
        String iterationNr = args[2];
        boolean mergeShortDistanceBins = Boolean.valueOf(args[3]);

        String eventsFile = dir + runId + "/output/ITERS/it."+iterationNr+"/" +runId +"."+iterationNr+".events.xml.gz";
        String configFile = dir + runId + "/output/"+runId+".output_config.xml";
        String outputFile = dir + runId + "/output/ITERS/it."+iterationNr+"/" + runId +"."+iterationNr+".multiMode_distanceDistributionCounts_absolute_offline.txt";

        Config config = ConfigUtils.loadConfig(configFile);
        config.network().setInputFile( runId+".output_network.xml.gz");
        config.plans().setInputPersonAttributeFile(null);
        config.plans().setInputFile(runId+".output_plans.xml.gz");
        config.counts().setInputFile(null);
        config.vehicles().setVehiclesFile(null);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        FacilitiesConfigGroup facilitiesConfigGroup = scenario.getConfig().facilities();
        FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenario.getActivityFacilities(), facilitiesConfigGroup);
        facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(true, scenario.getNetwork());
        facilitiesFromPopulation.assignOpeningTimes(facilitiesConfigGroup.isAssigningOpeningTime(), scenario.getConfig().planCalcScore());
        facilitiesFromPopulation.run(scenario.getPopulation());

        EventsManager eventsManager = EventsUtils.createEventsManager();

        DistanceDistribution distri = NEMOUtils.getDistanceDistribution(scenario.getConfig().counts().getCountsScaleFactor(), scenario.getConfig().plansCalcRoute(), mergeShortDistanceBins,
                true);

        BeelineDistanceCollector collector = new BeelineDistanceCollector(scenario, distri, eventsManager, new RuhrAgentsFilter(scenario, NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE ));

        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        writeData(outputFile, collector.getOutputDistanceDistribution(), distri);
    }

    private static void writeData (String filename, DistanceDistribution averages, DistanceDistribution inputDistanceDistribution){
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write(DistanceDistributionUtils.DistanceDistributionFileLabels.mode + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.distanceLowerLimit + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.distanceUpperLimit + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.measuredCount + "\t" +
                    "simulationCount");
            writer.newLine();

            for (Entry<ModalDistanceBinIdentifier, DistanceBin> entry : DistanceDistributionUtils.getSortedMap(averages).entrySet()) {
                writer.write(
                        entry.getKey().getMode() + "\t"
                                + entry.getKey().getDistanceRange().getLowerLimit() + "\t"
                                + entry.getKey().getDistanceRange().getUpperLimit() + "\t" +
                                inputDistanceDistribution.getModalBinToDistanceBin()
                                                              .get(entry.getKey().getId())
                                                              .getCount() + "\t" +
                                entry.getValue().getCount() * inputDistanceDistribution.getModeToScalingFactor().get(entry.getKey().getMode()) );
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data is not written. Reason :" + e);
        }
    }
}
