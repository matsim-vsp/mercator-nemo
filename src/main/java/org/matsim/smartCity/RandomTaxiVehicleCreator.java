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

package org.matsim.smartCity;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.nemo.runners.smartCity.SmartCityShpUtils;

import java.util.*;

/**
 * @author ikaddoura
 *
 */
public class RandomTaxiVehicleCreator {
	private static final Logger log = Logger.getLogger(RandomTaxiVehicleCreator.class);

	private final String vehiclesFilePrefix;
	// private final CoordinateTransformation ct;

	private final Scenario scenario;
	private final Random random = MatsimRandom.getRandom();
	private final String consideredMode = "car";
	private final SmartCityShpUtils shpUtils;

	public static void main(String[] args) {

		String networkFile = "C://Users//Gregor//Desktop//RuhrScenario//ruhrgebiet-v1.0-network-with-RSV.xml.gz";
		String drtServiceAreaShapeFile = "C://Users/Gregor/Desktop/RuhrScenario/ruhrgebiet_boundary.shp";
		String vehiclesFilePrefix = "nemoSmartCity";
		int numberOfVehicles = 300;
		int seats = 6;

		RandomTaxiVehicleCreator tvc = new RandomTaxiVehicleCreator(networkFile, drtServiceAreaShapeFile,
				vehiclesFilePrefix);
		tvc.run(numberOfVehicles, seats);
	}

	public RandomTaxiVehicleCreator(String networkfile, String drtServiceAreaShapeFile, String vehiclesFilePrefix) {
		this.vehiclesFilePrefix = vehiclesFilePrefix;
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkfile);
		this.scenario = ScenarioUtils.loadScenario(config);
		Set<String> modes = new HashSet<>();
		modes.add(consideredMode);
		new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);

		// change nodes in a way that pt links are not taken into consideration

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getId().toString().startsWith("pt_")) {
				link.getFromNode().setCoord(new Coord(4449458.462246102, 4708888.241959611));
				link.getToNode().setCoord(new Coord(4449458.462246102, 4708888.241959611));
			}
		}
		shpUtils = new SmartCityShpUtils(drtServiceAreaShapeFile);
	}

	public final void run(int amount, int seats) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

		for (int i = 0; i < amount; i++) {
			Link link = null;

			while (link == null) {
				Point p = shpUtils.getRandomPointInServiceArea(random);
				link = NetworkUtils.getNearestLinkExactly(scenario.getNetwork(), MGC.point2Coord(p));
				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord())
						&& shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					if (link.getAllowedModes().contains(consideredMode)) {
						// ok
					} else {
						link = null;
					}
					// ok, the link is within the shape file
				} else {
					link = null;
				}
			}

			if (i % 5000 == 0)
				log.info("#" + i);

			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("rt" + i, DvrpVehicle.class))
					.startLinkId(link.getId()).capacity(seats).serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600)).build());
		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + "veh_" + seats + "seats.xml.gz");
	}

}
