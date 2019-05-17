/* *********************************************************************** *
 * project: org.matsim.*cd 
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

package org.matsim.smartCity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.ReducePopulation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.run.RunRuhrgebietScenario;

/**
 * This class starts a simulation run with DRT.
 * 
 * - The input DRT vehicles file specifies the number of vehicles and the
 * vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing). -
 * The DRT service area is set to the the Ruhrgebiet area (see input
 * shape file). - Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public final class RunRuhrgebietSmartCityScenario {

	private static final Logger log = Logger.getLogger(RunRuhrgebietSmartCityScenario.class);

	public static final String DRT_SERVICE_AREA_SHAPE_FILE = "ruhrgebiet_boundary";

	public static final String drtServiceAreaAttribute = "drtServiceArea";
	private final String drtNetworkMode = TransportMode.car;

	private final String drtServiceAreaShapeFile;

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunRuhrgebietScenario ruhrgebiet;

	private boolean hasPreparedConfig = false;
	private boolean hasPreparedScenario = false;
	private boolean hasPreparedControler = false;

	public static void main(String[] args) throws CommandLine.ConfigurationException {
		new RunRuhrgebietSmartCityScenario(args).run();
	}

	public RunRuhrgebietSmartCityScenario(String[] args) throws CommandLine.ConfigurationException {
		if (args.length != 0) {
			CommandLine cmd = new CommandLine.Builder(args).allowPositionalArguments(false)
					.requireOptions(RunRuhrgebietScenario.CONFIG_PATH, DRT_SERVICE_AREA_SHAPE_FILE).build();

			this.drtServiceAreaShapeFile = cmd.getOptionStrict(DRT_SERVICE_AREA_SHAPE_FILE);
			this.ruhrgebiet = new RunRuhrgebietScenario(args);

		} else {
			
			String configFileName = "C://Users//Gregor//Desktop//RuhrScenario//ruhrgebiet-v1.0-1pct.configDRT.xml/";
			//String configFileName = "/net/homes/ils/rybczak/NemoSmartCity/Input/ruhrgebiet-v1.0-1pct.configDRT.xml";
			//this.drtServiceAreaShapeFile = "/net/homes/ils/rybczak/NemoSmartCity/Input/ruhrgebiet_boundary.shp";
			this.drtServiceAreaShapeFile = "C://Users//Gregor//Desktop//RuhrScenario//ruhrgebiet_boundary.shp";
			//this.drtServiceAreaShapeFile = DRT_SERVICE_AREA_SHAPE_FILE;
			this.ruhrgebiet = new RunRuhrgebietScenario(new String[] { "--config-path", configFileName,
					"--" + DRT_SERVICE_AREA_SHAPE_FILE, drtServiceAreaShapeFile });
		}
	}

	public Controler prepareControler() {
		if (!hasPreparedScenario) {
			prepareScenario();
		}

		controler = ruhrgebiet.prepareControler();

		// drt + dvrp module
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(
				DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));

		// reject drt requests outside the service area
		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(config).getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class)
						.toInstance(new ServiceAreaRequestValidator(drtServiceAreaAttribute));
			}
		});

		// Add drt-specific fare module
		controler.addOverridingModule(new DrtFareModule());
		hasPreparedControler = true;
		return controler;
	}

	public Scenario prepareScenario() {
		if (!hasPreparedConfig) {
			prepareConfig();
		}

		scenario = ruhrgebiet.prepareScenario();

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		SmartCityShpUtils shpUtils = new SmartCityShpUtils(drtServiceAreaShapeFile);
		new SmartCityNetworkModification(shpUtils).addSAVmode(scenario, drtNetworkMode, drtServiceAreaAttribute);

		hasPreparedScenario = true;
		return scenario;
	}

	public Config prepareConfig(ConfigGroup... modulesToAdd) {

		// dvrp, drt config groups
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new DrtConfigGroup());
		drtModules.add(new DrtFaresConfigGroup());

		List<ConfigGroup> modules = new ArrayList<>();
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}

		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = ruhrgebiet.prepareConfig(modules.toArray(modulesArray));
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.qsim().setNumberOfThreads(1); // drt is still single threaded!
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
		hasPreparedConfig = true;
		return config;
	}

	public void run() {
		if (!hasPreparedControler) {
			prepareControler();
		}

		Network network;
		NetworkFilterManager networkFilterManager = new NetworkFilterManager(scenario.getNetwork());
		networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().equals(TransportMode.bike)) {
					return false;
				}
				else if (l.getAllowedModes().contains(TransportMode.car) && l.getAllowedModes().contains(TransportMode.ride)){
					return true;
				}
				else {
					return true;
				}
		}});

		network = networkFilterManager.applyFilters();
		XY2Links xy2Links = new XY2Links(network, scenario.getActivityFacilities());
		for (Person p : scenario.getPopulation().getPersons().values()) {
			xy2Links.run(p);
		}

        NetworkWriter writer = new NetworkWriter(network);
		writer.write("C://Users//Gregor//Desktop//filteredNetwork.xml");

		//controler.run();
		log.info("Done.");
	}
}
