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

import org.apache.log4j.Logger;
import playground.vsp.corineLandcover.CORINELandCoverCoordsModifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by amit on 24.10.17.
 *
 * - takes matsim plans, CORINE shape file and zonal shape file as input
 * - check for every activity location, if they are inside the correct zone based on activity type
 * - if not, reassign the coordinate.
 *
 * Update(Feb'18): Reassigning is removed from this to avoid confusion,
 * if any coord is not a fake coord, an exception will be thrown.
 */

public class PlansModifierForCORINELandCover {

    private static final Logger LOG = Logger.getLogger(PlansModifierForCORINELandCover.class);
//    private static final Coord fakeCoord = new Coord(-1,-1);

//    private final CorineLandCoverData corineLandCoverData;
//    private final Population population;

//    private final Map<String, Geometry> zoneFeatures = new HashMap<>();

//    private final boolean sameHomeActivity ;

//    /**
//     * For this, it is assumed that home activity location is same in all plans of a person. If this is not the case, use other constructor.
//     */
//    public PlansModifierForCORINELandCover(String matsimPlans, Map<String, String> shapeFileToFeatureKey, String CORINELandCoverFile, boolean simplifyGeoms, boolean combiningGeoms) {
//        this(matsimPlans, shapeFileToFeatureKey, CORINELandCoverFile, simplifyGeoms, combiningGeoms, true);
//    }
//
//    public PlansModifierForCORINELandCover(String matsimPlans, Map<String, String> shapeFileToFeatureKey, String CORINELandCoverFile, boolean simplifyGeoms, boolean combiningGeoms, boolean sameHomeActivity) {
//        this.corineLandCoverData = new CorineLandCoverData(CORINELandCoverFile, simplifyGeoms, combiningGeoms);
//        LOG.info("Loading population from plans file "+ matsimPlans);
//        this.population = getPopulation(matsimPlans);
//
//        for (String shapeFile : shapeFileToFeatureKey.keySet()) {
//            String key =shapeFileToFeatureKey.get(shapeFile);
//            LOG.info("Processing zone file "+ shapeFile + " with feature key "+ key);
//            Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
//            for (SimpleFeature feature : features) {
//                Geometry geometry = (Geometry)feature.getDefaultGeometry();
//                String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString((String) feature.getAttribute(key));
//                if (zoneFeatures.get(shapeId) !=null) { // union geoms corresponding to same zone id.
//                    zoneFeatures.put(shapeId, GeometryUtils.combine(Arrays.asList(geometry, zoneFeatures.get(shapeId))));
//                } else {
//                    zoneFeatures.put(shapeId, geometry);
//                }
//            }
//        }
//
//        this.sameHomeActivity = sameHomeActivity;
//        if (this.sameHomeActivity) LOG.info("Home activities for a person will be at the same location.");
//    }

    public static void main(String[] args) {

        String corineLandCoverFile = "data/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "data/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        String zoneIdTag = "KN";
        String matsimPlans = "data/input/plans/2018_jan_24/mergedPlansFiles/plans_1pct_fullChoiceSet.xml.gz";
        boolean simplifyGeom = true;
        boolean combiningGeoms = false;
        boolean sameHomeActivity = true;
        String outPlans = "data/input/plans/2018_jan_24/plans_1pct_fullChoiceSet_coordsAssigned2.xml.gz";

        String spatialRefinementShapeFile = "data/cemdap_input/shapeFiles/plzBasedPopulation/plz-gebiete_Ruhrgebiet/plz-gebiete_Ruhrgebiet_withPopulation.shp";
        String featureKeySpatialRefinement = "plz";
        String homeActivityPrefix = "home";

        if(args.length > 0){
            corineLandCoverFile = args[0];
            zoneFile = args[1];
            zoneIdTag = args[2];
            spatialRefinementShapeFile = args[3];
            featureKeySpatialRefinement = args[4];
            matsimPlans = args[5];
            simplifyGeom = Boolean.valueOf(args[6]);
            combiningGeoms = Boolean.valueOf(args[7]);
            sameHomeActivity = Boolean.valueOf(args[8]);
            homeActivityPrefix = args[9];
            outPlans = args[10];
        }

        Map<String, String> shapeFileToFeatureKey = new HashMap<>();
        shapeFileToFeatureKey.put(zoneFile, zoneIdTag);
        shapeFileToFeatureKey.put(spatialRefinementShapeFile, featureKeySpatialRefinement);

        CORINELandCoverCoordsModifier plansFilterForCORINELandCover = new CORINELandCoverCoordsModifier(
                matsimPlans,
                shapeFileToFeatureKey,
                corineLandCoverFile,
                simplifyGeom,
                combiningGeoms,
                sameHomeActivity,
                homeActivityPrefix
                );
        plansFilterForCORINELandCover.process();
        plansFilterForCORINELandCover.writePlans(outPlans);
    }

//    public void process() {
//        LOG.info("Start processing, this may take a while ... ");
//        for(Person person : population.getPersons().values()) {
//            Coord homeLocationCoord = null;
//            for (Plan plan : person.getPlans()){
//                for (PlanElement planElement : plan.getPlanElements()){
//                    if (planElement instanceof Activity) {
//                        Activity activity = (Activity) planElement;
//                        String activityType = activity.getType().split("_")[0];
//                        String zoneId = activity.getType().split("_")[1];
//                        Coord coord = activity.getCoord();
//
//                        // during matsim plans generation, for home activities following fake coordinate is assigned.
//                        if (coord.equals(fakeCoord)) coord=null;
//                        else throw new RuntimeException("Coord must be a fake coord. Desired coord is "+ fakeCoord.toString()+ " whereas, actual coord is "+coord.toString());
//
//
//                        if (activityType.equals("home") && sameHomeActivity) {
//                            if (homeLocationCoord==null) {
//                                coord = getRandomCoord(LandCoverUtils.LandCoverActivityType.home, zoneId);
//                                activity.setCoord(coord);
//                                homeLocationCoord = coord;
//                            } else {
//                                activity.setCoord(homeLocationCoord);
//                            }
//                        } else {
////                            if (coord ==null) {
//                                coord = getRandomCoord(LandCoverUtils.LandCoverActivityType.other, zoneId);
//                                activity.setCoord(coord);
////                            } else {
////                                Point point = MGC.coord2Point(coord);
////
////                                if (! corineLandCoverData.isPointInsideLandCover(activityType, point) ){
////                                    activity.setCoord(reassignCoord(point,activityType));
////                                }
////                            }
//                            // get a coord if it is null
//                            // assign new coord if it is not null and not in the given feature
//                        }
//                        activity.setType(activityType);
//                    }
//                }
//            }
//        }
//        LOG.info("Finished processing.");
//    }

//    public void writePlans(String outFile){
//        LOG.info("Writing resulting plans to "+outFile);
//        new PopulationWriter(population).write(outFile);
//    }
//
//    /**
//     *
//     * @param activityType
//     * @param zoneId
//     * @return a random coord if there was no coordinate assigned to activity already.
//     */
//    private Coord getRandomCoord(LandCoverUtils.LandCoverActivityType activityType, String zoneId){
//        Geometry zone = this.zoneFeatures.keySet()
//                                         .stream()
//                                         .filter(key -> key.equals(zoneId))
//                                         .findFirst()
//                                         .map(this.zoneFeatures::get)
//                                         .orElse(null);
//        return corineLandCoverData.getRandomCoord(zone, activityType);
//    }
//
////    /**
////     * @param point
////     * @param activityType
////     * @return a random coord if there was already a coordinate assigned to activity, i.e.
////     * already assigned coord is inside the zone (municipality/plz/lor) but not in the CORINE landcover zone for given activity type.
////     */
////    private Coord reassignCoord(Point point, String activityType){
////        Geometry zone = this.zoneFeatures.values()
////                                         .stream()
////                                         .filter(geometry -> geometry.contains(point))
////                                         .findFirst()
////                                         .orElse(null);
////        return corineLandCoverData.getRandomCoord(zone, activityType);
////    }
//
//    private Population getPopulation (String plansFile) {
//        Config config = ConfigUtils.createConfig();
//        config.plans().setInputFile(plansFile);
//        return ScenarioUtils.loadScenario(config).getPopulation();
//    }
}
