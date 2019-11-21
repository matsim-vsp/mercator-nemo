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

package org.matsim.nemo.runners.smartCity;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ikaddoura
 */

public final class SmartCityNetworkModification {
    private static final Logger log = Logger.getLogger(SmartCityNetworkModification.class);

    public static void addDrtModeAndMarkServiceArea(Network network, Collection<Geometry> serviceArea, String taxiNetworkMode, String serviceAreaAttribute) {

        log.info("Add taxi mode to allowed modes where car and ride is allowed. If in service area add service area attribute to link");
        network.getLinks().values().parallelStream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car) && link.getAllowedModes().contains(TransportMode.ride))
                .forEach(link -> {

                    // copy all previous modes and add taxiNetworkMode
                    Set<String> modes = link.getAllowedModes();
                    Set<String> newModes = new HashSet<>(modes);
                    newModes.add(taxiNetworkMode);
                    link.setAllowedModes(newModes);

                    // mark link to be in service area
                    addServiceAreaAttribute(link, serviceArea, serviceAreaAttribute);
                });
        log.info("Added drt mode to allowed modes and marked links with service area attribute");
    }

    private static void addServiceAreaAttribute(Link link, Collection<Geometry> serviceArea, String attributeKey) {

        if (isInGeometry(link.getCoord(), serviceArea))
            link.getAttributes().putAttribute(attributeKey, true);
        else
            link.getAttributes().putAttribute(attributeKey, false);
    }

    private static boolean isInGeometry(Coord coord, Collection<Geometry> geometry) {

        Point point = MGC.coord2Point(coord);
        return geometry.stream().anyMatch(geometry1 -> geometry1.contains(point));
    }
}
