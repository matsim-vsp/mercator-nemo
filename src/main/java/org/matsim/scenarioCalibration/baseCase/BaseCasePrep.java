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

package org.matsim.scenarioCalibration.baseCase;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * Created by amit on 09.07.18.
 */

public class BaseCasePrep {

    private static final Logger LOGGER = Logger.getLogger(BaseCasePrep.class);

    public static void main(String[] args) {

        // 01_Ruhr Detailed network
//        {
//            RuhrDetailedNetworkGenerator.main(new String [] {"true","true", "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/" });
//        }

        // 02_selected plans only... clear routes, linkInfo
        // also remove initial_plans attribute
//        {
//            //TODO not final version. Amit 09 July 2018
//            String plansFile = "../runs-svn/nemo/marginals/run291_a/output/run291_a.output_plans.xml.gz";
//            String outPopulationFileName = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/cadytsCalib_outputSlectedPlansOnly_run291_a.xml.gz";
//            Population outPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
//
//            Population inPopulation = NEMOUtils.loadScenarioFromPlans(plansFile).getPopulation();
//
//            for (Person person : inPopulation.getPersons().values()) {
//                Plan inSelectedPlan = person.getSelectedPlan();
//                clearLinkInfo( inSelectedPlan );
//                inSelectedPlan.getAttributes().removeAttribute(InitialPlanKeeperPlanRemoval.plan_attribute_name);
//
//                Person outPerson = outPopulation.getFactory().createPerson(person.getId());
//                outPerson.addPlan( inSelectedPlan );
//                outPopulation.addPerson(outPerson);
//            }
//
//            new PopulationWriter(outPopulation).write(outPopulationFileName);
//        }

        // extract PT network from
        {
            //TODO not final version. Amit 09 July 2018
            //PT transit schedule and transit network
        }


        // 03_facilityCleaner (not really necessary, can be reproduced.)
    }

    private static void clearLinkInfo(Plan plan) {

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = ((Activity)planElement);

                if (activity.getLinkId()==null) continue;

                if (activity.getCoord()!=null) activity.setLinkId(null);
                else if (activity.getFacilityId()!=null) activity.setLinkId(null);
                else {
                    throw new RuntimeException("Activity "+planElement+" has neither link nor facility id. This should not happen. Aborting...");
                }

            } else if (planElement instanceof Leg) {
                ((Leg)planElement).setRoute(null);
            }
        }

    }
}






