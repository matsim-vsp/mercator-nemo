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

package org.matsim.scenarioCalibration.modeChoice;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.corineLandcover.GeometryUtils;

/**
 * Take selected plans and persons who originate, terminate any trip to Ruhr boundary or pass through it.
 * This will also remove routes (see switch) so that a detailed network can be used.
 *
 * Created by amit on 12.02.18.
 */

public class FilterSimulatedNRWPopulationForRuhr {

    private static final String boundaryShape = NEMOUtils.Ruhr_BOUNDARY_SHAPE_FILE;

    private static final String plansFile = "../../repos/runs-svn/nemo/locationChoice/run9/output/run9.output_plans.xml.gz";
    private static final String networkFile = "../../repos/runs-svn/nemo/locationChoice/run9/output/run9.output_network.xml.gz";

    private static final String outPlansFile = "../../repos/runs-svn/nemo/modeChoice/input/plans_1pct_Ruhr_noRoutes.xml.gz";

    //some assumptions
    private static final boolean keepOnlySelectedPlans = true;
    private static final boolean removeRoute = true;
    //

    private final Geometry combinedGeom;
    private final Network network;
    private final Population outPopulation;

    FilterSimulatedNRWPopulationForRuhr() {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShape);
        this.combinedGeom = GeometryUtils.combine(features.stream()
                                                          .map(f -> (Geometry) f.getDefaultGeometry())
                                                          .collect(Collectors.toList()));
        this.network = NEMOUtils.loadScenarioFromNetwork(networkFile).getNetwork();
        this.outPopulation = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();
    }

    public static void main(String[] args) {
        Population inputPopulation = NEMOUtils.loadScenarioFromPlans(plansFile).getPopulation();
        new FilterSimulatedNRWPopulationForRuhr().processAndWritePlans(inputPopulation);
    }

    void processAndWritePlans(Population inputPopulation) {
        inputPopulation.getPersons().values().stream().filter(this::keepPerson).forEach(this::cloneAndAddPerson);

        if (removeRoute) {
            this.outPopulation.getPersons()
                              .values()
                              .stream()
                              .flatMap(p -> p.getSelectedPlan().getPlanElements().stream())
                              .forEach(pe -> {
                                  if (pe instanceof Leg){
                                      ((Leg) pe).setRoute(null);
                                  } else{
                                      Activity activity = (Activity) pe;
                                      //make sure coord is not null
                                      Gbl.assertNotNull( activity.getCoord() );
                                      activity.setLinkId(null);
                                  }
                              });
        } else{
            throw new RuntimeException("If a different network is used with the output plans, exceptions will be thrown.");
        }

        new PopulationWriter(this.outPopulation).write(outPlansFile);
    }

    private boolean keepPerson(Person person) {
        for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) { // take only selected plan
            if (planElement instanceof Activity) { // if person is making any trip to Ruhr, keep it
                if (combinedGeom.contains(MGC.coord2Point(((Activity) planElement).getCoord()))) {
                    return true;
                }
            } else if (planElement instanceof Leg) {
                NetworkRoute networkRoute = (NetworkRoute) ((Leg) planElement).getRoute(); // must be network route
                List<Id<Link>> route = networkRoute.getLinkIds();
                return route.stream()
                            .anyMatch(linkId -> combinedGeom.contains(MGC.coord2Point(this.network.getLinks()
                                                                                                  .get(linkId)
                                                                                                  .getCoord())));
            }
        }
        return false;
    }

    private void cloneAndAddPerson(Person person) { // only selected plan
        if (!keepOnlySelectedPlans) {
            throw new RuntimeException("not implemented yet.");
        }

        Person outPerson = this.outPopulation.getFactory().createPerson(person.getId());
        Plan outPlan = this.outPopulation.getFactory().createPlan();
        PopulationUtils.copyFromTo(person.getSelectedPlan(), outPlan);
        outPerson.addPlan(outPlan);
        this.outPopulation.addPerson(outPerson);
    }
}