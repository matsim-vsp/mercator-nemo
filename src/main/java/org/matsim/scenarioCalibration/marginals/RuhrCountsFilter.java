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


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.util.NEMOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

/**
 * Created by amit on 24.04.18.
 */

public class RuhrCountsFilter {

    public static void main(String[] args) {

        String inputCountsFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/05062018/NemoCounts_data_allCounts_Pkw_05June18.xml";
        String outputCountsFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/NemoCounts_data_allCounts_Pkw_05June18_filteredForRuhr.xml";
        String networkFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/2018-05-03_NRW_coarse_filteredcleaned_network.xml.gz";

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE);

        //input counts
        Counts<Link> counts = new Counts<>();
        MatsimCountsReader reader = new MatsimCountsReader(counts);
        reader.readFile(inputCountsFile);

        Network network = NEMOUtils.loadScenarioFromNetwork(networkFile).getNetwork();

        //outputCounts
        Counts<Link> filteredCounts = new Counts<>();
        filteredCounts.setDescription(counts.getDescription());
        filteredCounts.setName(counts.getName());
        filteredCounts.setYear(counts.getYear());

        counts.getCounts()
              .entrySet()
              .stream()
              .filter(e -> isLinkInsideFeatures(features, network.getLinks().get(e.getKey())))
              .forEach(e -> {
                  Count<Link> out = filteredCounts.createAndAddCount(e.getKey(), e.getValue().getCsLabel());
                  e.getValue().getVolumes().forEach((key, value) -> out.createVolume(key, value.getValue()));
              });

        new CountsWriter(filteredCounts).write(outputCountsFile);
    }

    private static boolean isLinkInsideFeatures(final Collection<SimpleFeature> features, final Link link) {
        GeometryFactory GF = new GeometryFactory();
        Coord coord = link.getCoord();
        Point geo = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
        return isPointInsideFeatures(features, geo);
    }

    private static boolean isPointInsideFeatures(final Collection<SimpleFeature> features, final Point point) {
        if (features.isEmpty()) throw new RuntimeException("Collection of features is empty.");
        for(SimpleFeature sf : features){
            if ( (playground.vsp.corineLandcover.GeometryUtils.getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(point) ) {
                return true;
            }
        }
        return false;
    }

}
