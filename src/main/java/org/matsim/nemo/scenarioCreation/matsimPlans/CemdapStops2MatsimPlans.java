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

package org.matsim.nemo.scenarioCreation.matsimPlans;

import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by amit on 22.09.17.
 */

public class CemdapStops2MatsimPlans {


    /**
     * The plan is now:
     * - generate the matsim_plans without any coordinates for each file of cempdap_output (i.e. same process 5 times)
     * - the zone information is added to the activity types e.g. home_510
     * - sample first matsim_plans
     * - take sampled plans, add other plans of the sampled persons from other matsim_plans file and combine them in a file
     * - add the acitivity locations based on CORINE land cover data and zone information
     */

    public static void main(String[] args) throws IOException {
        // Local use
        String cemdapDataRoot = "data/cemdap_output/2018_jan_19/";
        int numberOfFirstCemdapOutputFile = 200;
        int numberOfPlans = 1;
        int numberOfPlansFile = 200;
        String outputDirectory = "data/matsim_initial/" + numberOfPlansFile + "/";

        String zonalShapeFile = "data/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        String zoneIdTag = "KN";

        String spatialRefinementShapeFile = "data/cemdap_input/shapeFiles/plzBasedPopulation/plz-gebiete_Ruhrgebiet/plz-gebiete_Ruhrgebiet_withPopulation.shp";
        String featureKeySpatialRefinement = "plz";

        boolean allowVariousWorkAndEducationLocations = true;
        boolean addStayHomePlan = true;
        boolean useLandCoverData = false;
        String landCoverFile = "data/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String stopFile = "Stops.out";
        String activityFile = "Activity.out";
        boolean simplifyGeometries = false;
        boolean assignCoordinatesToActivities = false;
        boolean combiningGeoms = false;
        int activityDurationThreshold_s = 1800;

        // Server use
        if (args.length != 0) {
            numberOfFirstCemdapOutputFile = Integer.parseInt(args[0]);
            numberOfPlans = Integer.parseInt(args[1]);
            allowVariousWorkAndEducationLocations = Boolean.parseBoolean(args[2]);
            addStayHomePlan = Boolean.parseBoolean(args[3]);
            outputDirectory = args[4];
            zonalShapeFile = args[5];
            cemdapDataRoot = args[6];
            useLandCoverData = Boolean.parseBoolean(args[7]);
            landCoverFile = args[8];
            simplifyGeometries = Boolean.valueOf(args[9]);
            combiningGeoms = Boolean.valueOf(args[10]);
            assignCoordinatesToActivities = Boolean.valueOf(args[11]);

            spatialRefinementShapeFile = args[12];
            featureKeySpatialRefinement = args[13];

            activityDurationThreshold_s = Integer.parseInt(args[14]);
        }

        Map<String, String> shapeFileToFeatureKey = new HashMap<>();
        shapeFileToFeatureKey.put(zonalShapeFile, zoneIdTag);
        shapeFileToFeatureKey.put(spatialRefinementShapeFile, featureKeySpatialRefinement);

        CemdapOutput2MatsimPlansConverter.convert(cemdapDataRoot, numberOfFirstCemdapOutputFile, numberOfPlans, outputDirectory,
                shapeFileToFeatureKey, allowVariousWorkAndEducationLocations, addStayHomePlan,
                useLandCoverData, landCoverFile, stopFile, activityFile,simplifyGeometries, combiningGeoms, assignCoordinatesToActivities, activityDurationThreshold_s);
    }
}
