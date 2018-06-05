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

import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;

/**
 * Created by amit on 04.04.18.
 */

public class MatsimPlansMaximumPossibleShortDistanceTripsCounter {

    private static final double lowerLimit = 0.;
    private static final double upperLimit = 1000.;
    private static final double beelineDistanceFactor = 1.3;

    public static void main(String[] args) {

        String inputPlansFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_splitActivities_filteredForRuhr.xml.gz";

        Population population = NEMOUtils.loadScenarioFromPlans(inputPlansFile).getPopulation();

        // go through with all plans and take plan which has most number of short trips (0-1km)
        int shortTripsCounter = 0;

        for (Person person: population.getPersons().values()){
            int maxShortTripsForPerson = 0;

            for (Plan plan : person.getPlans()){
                int numberOfShortTrips = 0;

                Coord fromCoord = null;
                for (PlanElement pe : plan.getPlanElements()){
                    if (pe instanceof Activity) {
                      if (fromCoord ==null){
                          fromCoord = ((Activity)pe).getCoord();
                      } else{
                          Coord toCoord = ((Activity)pe).getCoord();
                          double dist = beelineDistanceFactor * NetworkUtils.getEuclideanDistance(fromCoord, toCoord);
                          if (dist > lowerLimit && dist<= upperLimit) numberOfShortTrips++;

                          fromCoord = toCoord;
                      }
                    }
                }
                if (maxShortTripsForPerson < numberOfShortTrips) {
                    maxShortTripsForPerson = numberOfShortTrips;
                }
            }
            shortTripsCounter += maxShortTripsForPerson;
        }

        // some pre-evaluated stats for the same file.
//        System.out.println("Maximum number of 0-1km trips while considering all plans of a person (beeline distance factor = 1.0) is 37943");
//        System.out.println("Maximum number of 0-1km trips while considering all plans of a person (beeline distance factor = 1.3) is 29722");

        System.out.println("Maximum number of trips which are > "+lowerLimit+ "km and <= "+upperLimit+" km while considering all plans of a person (beeline distance factor = "+beelineDistanceFactor+") is "+ shortTripsCounter);


    }


}
