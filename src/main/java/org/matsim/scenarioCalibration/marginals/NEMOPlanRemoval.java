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

package org.matsim.scenarioCalibration.marginals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;

/**
 * The idea is to keep the initial plans which are marked with 'true' corresponding to 'intial_plan_attribute_name' attribute.
 *
 * Created by amit on 11.06.18.
 */

public class NEMOPlanRemoval implements PlanSelector<Plan, Person> {

    public static final String nemo_plan_remover = "NEMO_PLANS_REMOVAL";
    public static final String plan_attribute_name = "initial_plan_attribute_name";
    public static final String plan_attribute_prefix = "initial_plan_attribute_";

    private final WorstPlanForRemovalSelector delegate;

    @Inject
    public NEMOPlanRemoval (StrategyConfigGroup strategyConfigGroup){
        this.delegate = new WorstPlanForRemovalSelector();
        if ( strategyConfigGroup.getMaxAgentPlanMemorySize() < 12) {
            Logger.getLogger(NEMOPlanRemoval.class).warn("A plans remover is used which keeps the initial plans or at least their copy \n " +
                    "and maximum number of plans in the choice set is limited to "+ strategyConfigGroup.getMaxAgentPlanMemorySize()+
            ".\n Lower number of plans in choice set is likely to end up in infinite loop. Setting it to 15.");
            strategyConfigGroup.setMaxAgentPlanMemorySize(15);
        }
    }

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
        Plan planForRemoval = null;
        List<Plan> temporarilyRemovedPlans = new ArrayList<>();

        // WorstPlansForRemovalSelector will always return same plan if no change to choice set.
        // let's remove it --> store it --> get next worst plan remove it
        // repeat the process until a suitable plan is found.
        //put all plans back to the choice set.
        // TODO: putting plans back may change the positions of the plans in the choice set, assuming that it will not change the behaviour somewhere else.
        while ( planForRemoval == null ) {
            planForRemoval = delegate.selectPlan(member);

            Object value = planForRemoval.getAttributes().getAttribute(plan_attribute_name);
            // check if there exists a plan with same index

            int occurrences = (int) member.getPlans()
                                          .stream()
                                          .filter(pl -> Objects.equals(pl.getAttributes().getAttribute(plan_attribute_name),
                                                  value))
                                          .count();
            if (occurrences==1) {
                temporarilyRemovedPlans.add(planForRemoval);
                member.getPlans().remove(planForRemoval);
                planForRemoval=null;
            } else {
                temporarilyRemovedPlans.forEach(member::addPlan);
                return planForRemoval;
            }
        }
        return planForRemoval;
    }

    }
