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

package org.matsim.scenarioCalibration.marginals.stayHomePlan;

import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * The idea is:
 * <li> remove the scores of the plans </li>
 * <li> set the preparatory iteration to about 50 (stabilized scores) </li>
 * <li> at the beginning of 'prepatory iteration' store max score from choice set </li>
 * <li> use stored score as 'score of stay home plan' in every iteration afterwards </li>
 *
 * Created by amit on 25.05.18.
 */

public class StayHomeScoreUpdater implements IterationStartsListener {

    private static final Logger LOG = Logger.getLogger(StayHomeScoreUpdater.class);

    @Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
    @Inject private CadytsConfigGroup cadytsConfigGroup; // cadyts plan effect starts at preparatoryIterations
    @Inject private Population population;
    @Inject private ControlerConfigGroup controlerConfigGroup;

    private static final String personStayHomePlanScore = "stay_home_plan_score";

    private Plan createOrGetStayHomePlan (Person person) {
        for (Plan plan : person.getPlans()) {
            if (plan.getPlanElements().size()==1) return plan;
        }
        Plan plan = this.population.getFactory().createPlan();
        // add an activity home_86400.0
        Activity existAct = (Activity) person.getPlans().get(0).getPlanElements().get(0);
        Activity activity = this.population.getFactory().createActivityFromCoord("home_86400.0", existAct.getCoord());
        activity.setLinkId(existAct.getLinkId());

        // also add in activityParams if does not exists
        PlanCalcScoreConfigGroup.ActivityParams ap = this.planCalcScoreConfigGroup.getActivityParams(activity.getType());
        if (ap == null) {
            LOG.warn("Adding an activity of type "+activity.getType()+" with typical duration of 86400.0");
            ap = new PlanCalcScoreConfigGroup.ActivityParams(activity.getType());
            ap.setTypicalDuration(86400.0);
            ap.setTypicalDurationScoreComputation(PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative);
            this.planCalcScoreConfigGroup.addActivityParams(ap);
        }
        plan.addActivity(activity);
        person.addPlan(plan);
        return plan;
    }

//    private double getUtilPerf_stayHomePlan(Plan plan){
//        String actType = ((Activity)plan.getPlanElements().get(0) ).getType();
//
//        double typDur = this.planCalcScoreConfigGroup.getActivityParams(actType).getTypicalDuration();
//        switch (this.planCalcScoreConfigGroup.getActivityParams(actType).getTypicalDurationScoreComputation()) {
//            case uniform:
//                return  10.0 * this.planCalcScoreConfigGroup.getPerforming_utils_hr();
//            case relative:
//                return  (typDur / 3600.) * this.planCalcScoreConfigGroup.getPerforming_utils_hr();
//        }
//        throw new RuntimeException("Activity type "+actType+" not found.");
//    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if ( event.getIteration() ==  this.controlerConfigGroup.getFirstIteration() + this.cadytsConfigGroup.getPreparatoryIterations()){
            // store person stay home plan score here..capped by util_perf
            for (Person person : this.population.getPersons().values()) {
                double maxScoreOfPlans = person.getPlans()
                                        .stream()
                                        .map(BasicPlan::getScore)
                                        .reduce(Double.MIN_VALUE, Double::max);
                Plan plan = createOrGetStayHomePlan(person);
                plan.setScore( maxScoreOfPlans);
                this.population.getPersonAttributes().putAttribute(person.getId().toString(), personStayHomePlanScore, maxScoreOfPlans);
            }
        } else if ( event.getIteration() > this.controlerConfigGroup.getFirstIteration() + this.cadytsConfigGroup.getPreparatoryIterations()) {
            // put stay home plan in there if does not exists
            this.population.getPersons().values().forEach(p -> {
                double score = (double) this.population.getPersonAttributes().getAttribute(p.getId().toString(), personStayHomePlanScore);
                createOrGetStayHomePlan(p).setScore(score);
            });
        }
    }
}
