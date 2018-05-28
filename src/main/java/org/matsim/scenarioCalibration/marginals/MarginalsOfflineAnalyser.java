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

import java.io.BufferedWriter;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

/**
 * Created by amit on 28.05.18.
 */

public class MarginalsOfflineAnalyser {


    public static void main(String[] args) {

        String runId = "run249_SHP";
        String eventsFile = "../../repos/runs-svn/nemo/marginals/"+runId+"/output/"+runId+".output_events.xml.gz";
        String configFile = "../../repos/shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/preparedConfig.xml";
        String outputFile = "../../repos/runs-svn/nemo/marginals/"+runId+"/correctedDistriCompare.txt";

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));

        EventsManager eventsManager = EventsUtils.createEventsManager();

        DistanceDistribution distri = NEMOUtils.getDistanceDistribution(scenario.getConfig().counts().getCountsScaleFactor());

        BeelineDistanceCollector collector = new BeelineDistanceCollector(scenario.getNetwork(), scenario.getConfig().plansCalcRoute(), distri, eventsManager);
//        collector.setAgentFilter( new RuhrAgentsFilter(scenario.getPopulation()) );

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

            for (Entry<ModalDistanceBinIdentifier, DistanceBin> entry : getSortedMap(averages).entrySet()) {
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

    //technically, following can be applied directly in DistanceDistribution, however, ordering is imp here only.
    private static  SortedMap<ModalDistanceBinIdentifier, DistanceBin> getSortedMap(DistanceDistribution distanceDistribution) {
        SortedMap<ModalDistanceBinIdentifier, DistanceBin> sortedMap = new TreeMap<>();
        distanceDistribution.getModalBinToDistanceBin()
                            .forEach((key, value) -> sortedMap.put(distanceDistribution.getModalBins().get(key),
                                    value));
        return sortedMap;
    }
}
