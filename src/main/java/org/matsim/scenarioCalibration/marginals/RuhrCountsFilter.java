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

import java.util.Collection;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.geometry.GeometryUtils;

/**
 * Created by amit on 24.04.18.
 */

public class RuhrCountsFilter {

    public static void main(String[] args) {

        String inputCountsFile = "../../repos/shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/NemoCounts_data_allCounts_Pkw_24Nov2017.xml";
        String outputCountsFile = "../../repos/shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/NemoCounts_data_allCounts_Pkw_24Nov2017_filteredForRuhr.xml";
        String networkFile = "../../repos/shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-01_RuhrCalibration_withMarginals/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz";

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE);

        //input counts
        Counts<Link> counts = new Counts<>();
        MatsimCountsReader reader = new MatsimCountsReader(counts);
        reader.readFile(inputCountsFile);

        Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

        //outputCounts
        Counts<Link> filteredCounts = new Counts<>();
        filteredCounts.setDescription(counts.getDescription());
        filteredCounts.setName(counts.getName());
        filteredCounts.setYear(counts.getYear());

        counts.getCounts()
              .entrySet()
              .stream()
              .filter(e -> GeometryUtils.isLinkInsideFeatures(features, network.getLinks().get(e.getKey())))
              .forEach(e -> {
                  Count<Link> out = filteredCounts.createAndAddCount(e.getKey(), e.getValue().getCsLabel());
                  e.getValue().getVolumes().forEach((key, value) -> out.createVolume(key, value.getValue()));
              });

        new CountsWriter(filteredCounts).write(outputCountsFile);
    }

}
