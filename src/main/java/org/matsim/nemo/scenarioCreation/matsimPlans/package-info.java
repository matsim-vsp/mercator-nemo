/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * Created by amit on 25.10.17.
 *
 * So, to generate the MATSim initial plans, the steps are:
 *
 * <ul>
 *     <li>For each cempdap output files set, create one MATSim plans file (use CempdapStops2MatsimPlans).</li>
 *     <li>In this step, coordinates are not assigned to activities,
 *     rather zone id is concatenated to the activity type by "_" for later use.</li>
 *     <li>Sample first MATSim plans file (use playground/dziemke/utils/PlanFileModifier).</li>
 *     <li>For every person in the sampled plans file, add other plans from other (4)
 *     MATSim plans file, thus, every person in the output plans will have 5 persons in the choice set (merge them using SampledPlansMerger).</li>
 *     <li>Generate coordinates for the activity locations using concatenated zone id,
 *     activity type and CORINE land cover, afterwards remove concatenated zone id from activity types (use PlansModifierForCORINELandCover).</li>
 * </ul>
 *
 */
package org.matsim.nemo.scenarioCreation.matsimPlans;
