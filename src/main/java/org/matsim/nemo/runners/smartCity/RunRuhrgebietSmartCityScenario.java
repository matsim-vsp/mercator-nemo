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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.run.RunRuhrgebietScenario;

/**
 * This class starts a simulation run with DRT.
 * <p>
 * - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * - The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * - Initial plans are not modified.
 *
 * @author ikaddoura
 */

public final class RunRuhrgebietSmartCityScenario {

	private static final Logger log = Logger.getLogger(RunRuhrgebietSmartCityScenario.class);

	public static final String DRT_SERVICE_AREA_SHAPE_FILE = "drt-service-area-shape-file";

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
					.requireOptions(RunRuhrgebietScenario.CONFIG_PATH, DRT_SERVICE_AREA_SHAPE_FILE)
					.build();

			this.drtServiceAreaShapeFile = cmd.getOptionStrict(DRT_SERVICE_AREA_SHAPE_FILE);
			this.ruhrgebiet = new RunRuhrgebietScenario(args);

		} else {

			String configFileName = "scenarios/berlin-v5.3-1pct/input/berlin-drtA-v5.3-1pct-Berlkoenig.config.xml";
			this.drtServiceAreaShapeFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";

			this.ruhrgebiet = new RunRuhrgebietScenario(
					new String[] { "--config-path", configFileName, "--" + DRT_SERVICE_AREA_SHAPE_FILE,
							drtServiceAreaShapeFile });
		}
	}

	public Controler prepareControler() {
		if (!hasPreparedScenario) {
			prepareScenario();
		}

		controler = ruhrgebiet.prepareControler();

		// drt + dvrp module
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(
				DrtConfigGroup.getSingleModeDrtConfig(controler.getConfig()).getMode()));

		// reject drt requests outside the service area
		controler.addOverridingQSimModule(
				new AbstractDvrpModeQSimModule(DrtConfigGroup.getSingleModeDrtConfig(config).getMode()) {
					@Override
					protected void configureQSim() {
						bindModal(PassengerRequestValidator.class).toInstance(
								new ServiceAreaRequestValidator(drtServiceAreaAttribute));
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

		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVmode(scenario, drtNetworkMode, drtServiceAreaAttribute);

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

		DrtConfigs.adjustDrtConfig(DrtConfigGroup.getSingleModeDrtConfig(config), config.planCalcScore());

		hasPreparedConfig = true;
		return config;
	}

	public void run() {
		if (!hasPreparedControler) {
			prepareControler();
		}

		controler.run();
		log.info("Done.");
	}
}

