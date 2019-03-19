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

package org.matsim;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.corineLandcover.GeometryUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by amit on 01.02.18.
 */

public class NEMOAreaFilter {

    private final Map<String, Geometry> nameToGeometries;

    public NEMOAreaFilter(Map<String, List<String>> nameToZonesList, String shapeFile, String featureKey) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        Map<String, List<Geometry>> nameToFeaturesList = new HashMap<>();

        for (SimpleFeature feature : features) {
            String munId = (String) feature.getAttribute(featureKey);
            nameToZonesList.entrySet()
                           .stream()
                           .filter(e -> e.getValue().contains(munId))
                           .forEach(e -> {
                               List<Geometry> zonalFeatures = nameToFeaturesList.getOrDefault(e.getKey(),
                                       new ArrayList<>());
                               zonalFeatures.add((Geometry) feature.getDefaultGeometry());
                               nameToFeaturesList.put(e.getKey(), zonalFeatures);
                           });
        }

        this.nameToGeometries = nameToFeaturesList.entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(e -> e.getKey(),
                                                        e -> GeometryUtils.combine(e.getValue())));
    }

    public String getAreaName(Coord coord){
        for (String name : this.nameToGeometries.keySet()) {
            if (this.nameToGeometries.get(name).contains(MGC.coord2Point(coord))){
                return name;
            }
        }
        return null; // ignore person/Trip
    }

}
