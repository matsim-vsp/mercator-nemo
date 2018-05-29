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

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.cadyts.marginals.AgentFilter;

/**
 * Created by amit on 28.05.18.
 */

public class RuhrAgentsFilter implements AgentFilter {

    public static final String ruhr_boundary_shape = "ruhr_boundary_shape";

    private static final Logger LOG = Logger.getLogger(RuhrAgentsFilter.class);

    private final Collection<SimpleFeature> features ;

    private final Population population;

    @Inject
    public RuhrAgentsFilter(Population population, @Named(RuhrAgentsFilter.ruhr_boundary_shape) String shapeFile) {
        this.population = population;
        this.features = ShapeFileReader.getAllFeatures(shapeFile);
    }

    @Override
    public boolean includeAgent(Id<Person> id) {
        Activity activity = (Activity) population.getPersons().get(id).getSelectedPlan().getPlanElements().get(0);
        if (! activity.getType().startsWith("home")) {
            LOG.warn("First activity is not type home. Excluding such agents...");
            return false;
        }
        for (SimpleFeature feature : features) {
            if ( ((Geometry)feature.getDefaultGeometry()).contains(MGC.coord2Point(activity.getCoord()))  ) {
                return true;
            }
        }
        return false;
    }
}
