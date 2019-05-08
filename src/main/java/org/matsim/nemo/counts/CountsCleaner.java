/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.nemo.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by amit on 07.11.17.
 */

public class CountsCleaner {


    public static void main(String[] args) {

        String inCountsFile = "data/input/counts/10112017/NemoCounts_data_allCounts_KFZ.xml";
        String outCountsFile = "data/input/counts/10112017/NemoCounts_data_allCounts_KFZ_cleanedForNetwork.xml";
        String networkFile = "data/input/network/allWaysNRW/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz";

        CountsCleaner.cleanCountsFile(inCountsFile, networkFile, outCountsFile);

    }

    private static void cleanCountsFile(String countsFile, String networkFile, String outCountsFile) {

        Counts<Link> counts = new Counts<>();
        CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
        countsReader.readFile(countsFile);

        System.out.println("Number of count stations are "+ counts.getCounts().size());

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);

        Network network = ScenarioUtils.loadScenario(config).getNetwork();

        Set<Id<Link>> countIds =  new HashSet<>();

        for(Id<Link> countId : counts.getCounts().keySet()) {
            if (! network.getLinks().containsKey(countId)) {
                System.out.println("Count id "+countId+ "not found in network file.");
                countIds.add(countId);
            }
        }

        countIds.forEach(e -> counts.getCounts().remove(e));

        System.out.println("Number of count stations after cleaning are "+ counts.getCounts().size());

        new CountsWriter(counts).write(outCountsFile);
    }
}
