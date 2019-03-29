/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class RemoveRouteInfoFromPlans {
	
	private static final Logger log = Logger.getLogger(RemoveRouteInfoFromPlans.class);
	
	private static String inputPlans = "/Users/ihab/Desktop/selectedPlans_from_baseCase_089.xml.gz";
	private static String outputPlans = "/Users/ihab/Desktop/selectedPlans_from_baseCase_089_without-routes.xml.gz";
//	private static final String[] attributes = {"OpeningClosingTimes"};
	private static final String[] attributes = {};
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			inputPlans = args[0];
			outputPlans = args[1];		
			log.info("input plans: " + inputPlans);
			log.info("output plans: " + outputPlans);
		}
		
		RemoveRouteInfoFromPlans filter = new RemoveRouteInfoFromPlans();
		filter.run(inputPlans, outputPlans, attributes);
	}
	
	public void run (final String inputPlans, final String outputPlans, final String[] attributes) {
		
		log.info("Accounting for the following attributes:");
		for (String attribute : attributes) {
			log.info(attribute);
		}
		log.info("Other person attributes will not appear in the output plans file.");
		
		Scenario scOutput;
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scInput = ScenarioUtils.loadScenario(config);
		
		scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		
		for (Person p : scInput.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			PopulationFactory factory = popOutput.getFactory();
			Person personNew = factory.createPerson(p.getId());
			
			for (String attribute : attributes) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}
			
			List<String> types = new ArrayList<>();
			types.add("car interaction");
			types.add("pt interaction");
			types.add("bike interaction");
			types.add("ride interaction");

			Plan plan = factory.createPlan();
			
			Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
			plan.addActivity(firstAct);
	
			StageActivityTypes stageActivities = new StageActivityTypesImpl(types);
			for (Trip trip : TripStructureUtils.getTrips(selectedPlan, stageActivities)) {			
				MainModeIdentifierOnlyTransitWalkImpl modeIdentifier = new MainModeIdentifierOnlyTransitWalkImpl();
				String mode = modeIdentifier.identifyMainMode(trip.getTripElements());			
				plan.addLeg(factory.createLeg(mode));
				plan.addActivity(trip.getDestinationActivity());
			}
												
//			for (PlanElement pE : selectedPlan.getPlanElements()) {
//				if (pE instanceof Leg) {
//					Leg leg = (Leg) pE;
//					
//					if (leg.getMode().equals(TransportMode.car)) {
//						leg.setRoute(null);
//					}
//					
//				}
//			}
			popOutput.addPerson(personNew);
			personNew.addPlan(plan);
		}
		
		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

