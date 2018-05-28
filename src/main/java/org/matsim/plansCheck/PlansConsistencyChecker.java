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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

/**
 * Created by amit on 28.05.18.
 */

public class PlansConsistencyChecker {

    private static final Logger LOG = Logger.getLogger(PlansConsistencyChecker.class);
    private static final int max_plans_choiceSet = 10;

    public static void main(String[] args) {

        String plansFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/plans_1pct_fullChoiceSet_coordsAssigned_splitActivities_filteredForRuhr.xml.gz";
        String configFile = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/preparedConfig.xml";
        String dir = "../shared-svn/projects/nemo_mercator/data/matsim_input/2018-05-28_shorterIntraZonalDist/stats/";

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(plansFile);

        Population population = ScenarioUtils.loadScenario(config).getPopulation();

        if (! new File(dir).exists()) new File(dir).mkdir();

        writeStayHomePlansInfo( population, dir);
        writeHomeZoneIdCheckStatus(population, dir);
        writeNumberOfSameActivityPatterns(population, dir);
    }

    private static void writeNumberOfSameActivityPatterns(Population population, String dir ){
        LOG.warn("Identifying number of occurrences for same activity patterns");
        int [] sameActivityPatters = new int [max_plans_choiceSet];

        for (Person person : population.getPersons().values()) {
            Map<String, Integer> actTripsToIndex = new HashMap<>();
            for (Plan plan :person.getPlans()){
                String actPattern = "";
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof  Activity) {
                        actPattern += ((Activity)planElement).getType() +"--" ;
                    }
                }
                int cnt = actTripsToIndex.getOrDefault(actPattern, 1);
                actTripsToIndex.put(actPattern, cnt+1);
            }
            int number = Collections.max(actTripsToIndex.entrySet(), Map.Entry.comparingByValue()).getValue();
            sameActivityPatters[number] = sameActivityPatters[number]+1;
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/sameActivityPatterns_stats.txt")) {
            writer.write("numberOfSameActivityPatternsForEveryPerson\tcount\n");
            for (int index = 0; index < sameActivityPatters.length; index++){
                writer.write(index+"\t"+sameActivityPatters[index]+"\n");
            }
        } catch (IOException e ){
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    private static void writeHomeZoneIdCheckStatus( Population population, String dir ){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/homeZoneIdCheck_stats.txt")) {
            writer.write("personId\thomeZoneId_firstAct\thomeZoneId_otherHomeAct\n");

            for(Person person : population.getPersons().values()){
                String homZoneId = null;
                for(Plan plan : person.getPlans()){
                    for(PlanElement pe : plan.getPlanElements()){
                        if (pe instanceof Activity){
                            Activity act = ((Activity)pe);
                            String [] parts = act.getType().split("_");
                            String activityType = parts[0];
                            String zoneId = (String) act.getAttributes().getAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey);

                            if (activityType.equals("home")){
                                if (homZoneId==null){
                                    homZoneId = zoneId;
                                } else {
                                    if (! homZoneId.equals(zoneId)){
                                        writer.write(person.getId()+"\t"+homZoneId+"\t"+zoneId+"\n");
                                        System.out.println("Different home zones for person "+ person.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e ){
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    private static void writeStayHomePlansInfo(Population population, String dir){
        int [] numberOfStayHomePlansInChoiceSet = new int[max_plans_choiceSet];//assuming maximum number of plans to 15 in choice set.

        for (Person person : population.getPersons().values()) {
            int number = (int) person.getPlans().stream().filter(plan -> plan.getPlanElements().size()==1).count();
            numberOfStayHomePlansInChoiceSet[number] = numberOfStayHomePlansInChoiceSet[number]+1;
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/stayHomePlans_stats.txt")) {
            writer.write("numberOfStayHomePlans\tcount\n");
            for (int index = 0; index < numberOfStayHomePlansInChoiceSet.length; index++){
                writer.write(index+"\t"+numberOfStayHomePlansInChoiceSet[index]+"\n");
            }
        } catch (IOException e ){
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

}
