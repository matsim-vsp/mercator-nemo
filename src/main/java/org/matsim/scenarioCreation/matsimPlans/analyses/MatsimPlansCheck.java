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

package org.matsim.scenarioCreation.matsimPlans.analyses;

import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
 * Created by amit on 06.02.18.
 */

public class MatsimPlansCheck {

    public static void main(String[] args) {
        String plansFile = "data/input/plans/2018_jan_24/mergedPlansFiles/plans_1pct_fullChoiceSet.xml.gz";
        new MatsimPlansCheck().checkForSameHomeCoord(plansFile);
    }

    private void checkForSameHomeCoord(String plansFile){
        Population population = NEMOUtils.loadScenarioFromPlans(plansFile).getPopulation();

        for(Person person : population.getPersons().values()){
            String homZoneId = null;
            for(Plan plan : person.getPlans()){
                for(PlanElement pe : plan.getPlanElements()){
                    if (pe instanceof Activity){
                        String [] parts = ((Activity)pe).getType().split("_");
                        String activityType = parts[0];
                        String zoneId = parts[1];

                        if (activityType.equals("home")){
                             if (homZoneId==null){
                                homZoneId = zoneId;
                            } else {
                                if (! homZoneId.equals(zoneId)){
                                    System.out.println("Different home zones for person "+ person.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Check is completed.");
    }
}
