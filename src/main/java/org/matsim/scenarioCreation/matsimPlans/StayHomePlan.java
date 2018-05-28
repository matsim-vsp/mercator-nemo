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

package org.matsim.scenarioCreation.matsimPlans;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

/**
 * Created by amit on 28.05.18.
 */

public class StayHomePlan {

    public static void main(String[] args) {

        String populationFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/zz_archive/plans/2018_05_28/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";
        String outPlans = "../shared-svn/projects/nemo_mercator/data/matsim_input/zz_archive/plans/2018_05_28/plans_1pct_fullChoiceSet_coordsAssigned_stayHomePlans.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(populationFile);

        Population sampledPop = ScenarioUtils.loadScenario(config).getPopulation();

        for (Person person : sampledPop.getPersons().values()) {
            Plan firstPlan = person.getPlans().get(0);
            Activity firstActivity = (Activity) firstPlan.getPlanElements().get(0); // Get first (i.e. presumably "home") activity from agent's first plan

            Plan stayHomePlan = sampledPop.getFactory().createPlan();
            // Create new activity with type and coordinates (but without end time) and add it to stay-home plan
            Activity Activity2 = sampledPop.getFactory().createActivityFromCoord(firstActivity.getType(), firstActivity.getCoord());
            Activity2.setCoord(firstActivity.getCoord());
            Activity2.setLinkId(firstActivity.getLinkId());
            Activity2.getAttributes().putAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey, firstActivity.getAttributes().getAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey));
            stayHomePlan.addActivity(Activity2);
            person.addPlan(stayHomePlan);
        }


        new PopulationWriter(sampledPop).write(outPlans);

    }


}
