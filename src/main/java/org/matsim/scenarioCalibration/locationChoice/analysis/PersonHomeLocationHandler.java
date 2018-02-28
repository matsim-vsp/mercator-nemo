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

package org.matsim.scenarioCalibration.locationChoice.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.NEMOAreaFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

/**
 * Created by amit on 01.02.18.
 */

public class PersonHomeLocationHandler implements PersonDepartureEventHandler {

    private final NEMOAreaFilter filter;
    private final Network network;

    private final Map<String, List<Id<Person>>> zoneNameToListOfPersons = new HashMap<>();

    PersonHomeLocationHandler(NEMOAreaFilter filter, Network network) {
        this.filter = filter;
        this.network = network;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String name = this.filter.getAreaName(this.network.getLinks().get(event.getLinkId()).getCoord());
        if (name != null) {
            List<Id<Person>> persons = this.zoneNameToListOfPersons.getOrDefault(name, new ArrayList<>());
            persons.add(event.getPersonId());
            this.zoneNameToListOfPersons.put(name, persons);
        }
    }

    @Override
    public void reset(int iteration) {
        this.zoneNameToListOfPersons.clear();
    }

    public Map<String, List<Id<Person>>> getZoneNameToListOfPersons() {
        return zoneNameToListOfPersons;
    }
}
