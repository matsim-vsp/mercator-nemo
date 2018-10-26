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

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.util.NEMOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.corineLandcover.GeometryUtils;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Take counting stations which are inside Ruhr, rest will anyways underestimated for population of Ruhr area.
 * <p>
 * Created by amit on 15.02.18.
 */

public class FilterNRWCountsForRuhe {

    private static final String boundaryShape = NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE;

    private static final String countsFile = "data/input/counts/24112017/NemoCounts_data_allCounts_Pkw.xml";
    private static final String networkFile = "../../repos/runs-svn/nemo/locationChoice/run9/output/run9.output_network.xml.gz";

    private static final String outCountsFile = "../../repos/runs-svn/nemo/modeChoice/input/counts_1pct_Ruhr.xml.gz";

    //TODO: fix the linkIds in counts based on the new network.

    private final Geometry combinedGeom;
    private final Network network;

    FilterNRWCountsForRuhe() {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShape);
        this.combinedGeom = GeometryUtils.combine(features.stream()
                                                          .map(f -> (Geometry) f.getDefaultGeometry())
                                                          .collect(Collectors.toList()));
        this.network = NEMOUtils.loadScenarioFromNetwork(networkFile).getNetwork();
    }

    public static void main(String[] args) {
        Counts<Link> inputCounts = new Counts<>();
        new MatsimCountsReader(inputCounts).readFile(countsFile);
        new FilterNRWCountsForRuhe().processAndWriteCountsFile(inputCounts);
    }

    void processAndWriteCountsFile(Counts<Link> inputCounts) {
        Counts<Link> outputCounts = new Counts<>();
        outputCounts.setName(inputCounts.getName());
        outputCounts.setYear(inputCounts.getYear());
        outputCounts.setDescription(inputCounts.getDescription());

        inputCounts.getCounts()
                   .entrySet()
                   .stream()
                   .filter(l -> linkInsideRuhr(l.getKey()))
                   .forEach(e -> outputCounts.getCounts().put(e.getKey(), e.getValue()));
        new CountsWriter( outputCounts).write(outCountsFile);
    }

    private boolean linkInsideRuhr(Id<Link> linkId) {
        return this.combinedGeom.contains(MGC.coord2Point(this.network.getLinks().get(linkId).getCoord()));
    }
}
