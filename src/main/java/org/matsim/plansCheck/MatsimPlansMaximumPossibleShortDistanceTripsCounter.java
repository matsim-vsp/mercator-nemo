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

package org.matsim.plansCheck;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/**
 * Created by amit on 04.04.18.
 */

public class MatsimPlansMaximumPossibleShortDistanceTripsCounter {

    private static final double lowerLimit = 0.;
    private static final double upperLimit = 1000.;
    private static final double beelineDistanceFactor = 1.3;

    private static final boolean writeOutputPopulation = false;
    private static final boolean createFacilities = true;

    public static void main(String[] args) {

//        String inputPlansFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_splitActivities_filteredForRuhr.xml.gz";
        String inputPlansFile = "../runs-svn/nemo/marginals/input/plans_1pct_filteredForRuhr_maxShortTrips.xml.gz";
        String inputNetwork = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/2018-05-03_NRW_coarse_filteredcleaned_network.xml.gz";
        String outPopulationFile = "../runs-svn/nemo/marginals/input/plans_1pct_filteredForRuhr_maxShortTrips.xml.gz";

        String facilityFile = "../runs-svn/nemo/marginals/input/plans_1pct_filteredForRuhr_maxShortTrips_facilities.xml.gz";

        Population outPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

        Config config =ConfigUtils.createConfig();

        config.plans().setInputFile(inputPlansFile);
        config.network().setInputFile(inputNetwork);
        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
//        config.facilities().setOneFacilityPerLink(false); // setting false meaning, one facility for each coord.

        Scenario scenario = ScenarioUtils.loadScenario(config);

        if (createFacilities) {
            FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenario.getActivityFacilities(), scenario.getConfig().facilities());
            facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(true, scenario.getNetwork());
            facilitiesFromPopulation.assignOpeningTimes(scenario.getConfig().facilities().isAssigningOpeningTime(), scenario.getConfig().planCalcScore());
            facilitiesFromPopulation.run(scenario.getPopulation());

            new FacilitiesWriter(scenario.getActivityFacilities()).write(facilityFile);

        } else {
            scenario.getConfig().facilities().setInputFile(facilityFile);
            ScenarioUtils.loadScenario(scenario);
        }

        Population population = scenario.getPopulation();
        ActivityFacilities activityFacilities = scenario.getActivityFacilities();

        // go through with all plans and take plan which has most number of short trips (0-1km)
        int shortTripsCounter = 0;

        for (Person person: population.getPersons().values()){
            int maxShortTripsForPerson = 0;
            Plan outPlan = null ;

            for (Plan plan : person.getPlans()){
                int numberOfShortTrips = 0;

                Coord fromCoord = null;
                for (PlanElement pe : plan.getPlanElements()){
                    if (pe instanceof Activity) {
                        Activity activity = ((Activity)pe);
                      if (fromCoord ==null){
                          fromCoord = activity.getCoord();
                          if (fromCoord==null) fromCoord = activityFacilities.getFacilities().get(activity.getFacilityId()).getCoord();
                      } else{
                          Coord toCoord = activity.getCoord();
                          if (toCoord==null) toCoord = activityFacilities.getFacilities().get(activity.getFacilityId()).getCoord();

                          double dist = beelineDistanceFactor * NetworkUtils.getEuclideanDistance(fromCoord, toCoord);
                          if (dist > lowerLimit && dist<= upperLimit) numberOfShortTrips++;

                          fromCoord = toCoord;
                      }
                    }
                }
                if (maxShortTripsForPerson < numberOfShortTrips) {
                    maxShortTripsForPerson = numberOfShortTrips;
                    outPlan = plan;
                }
            }

            Person outPerson = population.getFactory().createPerson(person.getId());
            AttributesUtils.copyAttributesFromTo(person, outPerson);

            if (outPlan!=null) {
                outPerson.addPlan(outPlan);
                outPopulation.addPerson(outPerson);
            } else{
                outPopulation.addPerson(person); // if there are no trips with 0-1 km..take all plans
            }

            shortTripsCounter += maxShortTripsForPerson;
        }

        // some pre-evaluated stats for the same file.
//        System.out.println("Maximum number of 0-1km trips while considering all plans of a person (beeline distance factor = 1.3) is 29705"); // from --> 2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_splitActivities_filteredForRuhr.xml.gz
        // System.out.println("Maximum number of 0-1km trips while considering all plans of a person (beeline distance factor = 1.3) is 29705"); // from --> plans_1pct_filteredForRuhr_maxShortTrips.xml.gz
        // if using activityFacilities with (oneFacilityPerLink=false) --> 29705



        System.out.println("Maximum number of trips which are > "+lowerLimit+ "km and <= "+upperLimit+" km while considering all plans of a person (beeline distance factor = "+beelineDistanceFactor+") is "+ shortTripsCounter);

        if (writeOutputPopulation) new PopulationWriter(outPopulation).write(outPopulationFile);

    }

}
