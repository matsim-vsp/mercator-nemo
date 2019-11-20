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

package org.matsim.nemo;


import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.cadyts.marginals.AgentFilter;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by amit on 28.05.18.
 */

public class RuhrAgentsFilter implements AgentFilter {

    private static final Logger logger = Logger.getLogger(RuhrAgentsFilter.class);
    private final Map<Id<Person>, Boolean> personIdMap;

    public RuhrAgentsFilter(Scenario scenario, Collection<SimpleFeature> shape) {

        final Function<Activity, Coord> coordProvider = getCoordProvider(scenario);

        logger.info("testing for all agents whether they have their home coord within the supplied shape");
        // go for the giant stream statement here, since the isInside shape test is expensive and we can parallelize this way
        personIdMap = scenario.getPopulation().getPersons().values().parallelStream()
                .filter(person -> person.getSelectedPlan() != null)
                .filter(person -> !person.getSelectedPlan().getPlanElements().isEmpty())
                .filter(person -> (person.getSelectedPlan().getPlanElements().get(0) instanceof Activity))
                .map(person -> Tuple.of(person.getId(), (Activity) person.getSelectedPlan().getPlanElements().get(0)))
                .filter(personActivity -> personActivity.getSecond().getType().startsWith("home"))
                .collect(Collectors.toMap(Tuple::getFirst, personActivity -> isActivityInside(coordProvider.apply(personActivity.getSecond()), shape)));
    }

    private static Function<Activity, Coord> getCoordProvider(Scenario scenario) {

        if (scenario.getActivityFacilities().getFacilities().isEmpty()) {
            return Activity::getCoord;
        } else {
            return activity -> scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
        }
    }

    private static boolean isActivityInside(Coord coord, Collection<? extends SimpleFeature> features) {

        return features.stream()
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .anyMatch(geometry -> geometry.contains(MGC.coord2Point(coord)));
    }

    @Override
    public boolean includeAgent(Id<Person> id) {
        return personIdMap.getOrDefault(id, false);
    }
}
