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

package org.matsim.nemo.scenarioCreation.matsimPlans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.nemo.util.NEMOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by amit on 29.01.18.
 */

public class PersonActivityLocationWriter {


    public static void main(String[] args) {

        String plansFile = "data/input/plans/2018_jan_24/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";

        Population population = NEMOUtils.loadScenarioFromPlans(plansFile).getPopulation();

        String outFile = "data/input/plans/2018_jan_24/plans_1pct_activityLocations.txt";

        BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outFile);
        try {
            writer.write("personId\tplanIndex\tactivityIndex\tactivityType\tlocationX\tlocationY\n");
            for (Person person : population.getPersons().values()) {
                for (int planIndex = 0; planIndex < person.getPlans().size(); planIndex++) {
                    int activityIndex = 0;
                    if (planIndex > 0) continue;
                    Plan plan = person.getPlans().get(planIndex);
                    for (PlanElement pe : plan.getPlanElements()) {
                        if (pe instanceof Activity) {
                            Activity act = ((Activity) pe);
                            Coord coord = act.getCoord();
                            writer.write(person.getId()
                                               .toString() + "\t" + planIndex + "\t" + activityIndex + "\t" + act.getType() + "\t" + coord
                                    .getX() + "\t" + coord.getY() + "\n");
                            activityIndex++;
                        }
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

}
