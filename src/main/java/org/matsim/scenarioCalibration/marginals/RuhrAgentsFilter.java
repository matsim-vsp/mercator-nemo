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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.cadyts.marginals.AgentFilter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by amit on 28.05.18.
 */

public class RuhrAgentsFilter implements AgentFilter {

    public static final String ruhr_boundary_shape = "ruhr_boundary_shape";

    private static final Logger LOG = Logger.getLogger(RuhrAgentsFilter.class);

    private final Collection<SimpleFeature> features ;

    private final Map<Id<Person>,Boolean> personIdMap  = new HashMap<>();

    @Inject
    public RuhrAgentsFilter(Scenario scenario, @Named(RuhrAgentsFilter.ruhr_boundary_shape) String shapeFile) {
        this.features = ShapeFileReader.getAllFeatures(shapeFile);
        ActivityFacilities activityFacilities = scenario.getActivityFacilities();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
            if (! activity.getType().startsWith("home")) {
                LOG.warn("First activity is not type home. Excluding such agents...");
                this.personIdMap.put(person.getId(), false);
            }
            if (activityFacilities.getFacilities().isEmpty()) {
                this.personIdMap.put(person.getId(), isActivityInside(activity.getCoord()));
            } else {
                this.personIdMap.put(person.getId(), isActivityInside( activityFacilities.getFacilities().get(activity.getFacilityId()).getCoord() ));
            }
        }
    }

    private boolean isActivityInside(Coord coord){
        for (SimpleFeature feature : features) {
            if ( ((Geometry)feature.getDefaultGeometry()).contains(MGC.coord2Point(coord))  ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean includeAgent(Id<Person> id) {
        // check for every agent gain and again is very very expensive and unnecessary
        if (this.personIdMap.containsKey(id)) {
            return this.personIdMap.get(id);
        }
        return false;
    }
}
