package org.matsim.nemo.runners;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.bicycle.Bicycles;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.nemo.util.NEMOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseCaseRunner {

	public static void main(String[] args) {

		new BaseCaseRunner().run(args);
	}

	static Controler loadControler(Scenario scenario) {

		Controler controler = new Controler(scenario);

		// use fast pt router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(AnalysisMainModeIdentifier.class).to(NemoModeLocationChoiceMainModeIdentifier.class);
			}
		});

		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

		// add bicycle module
		Bicycles.addAsOverridingModule(controler);

		return controler;
	}

	static Scenario loadScenario(Config config) {

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// the scenario generation uses bike_speed_factor, but the bicycle module expects something else
		// replace the attribute key with the bicycle contrib's key
		scenario.getNetwork().getLinks().values().parallelStream()
				.filter(link -> link.getAttributes().getAttribute("bike_speed_factor") != null)
				.forEach(link -> {
					var speedFactor = (Double) link.getAttributes().getAttribute("bike_speed_factor");
					link.getAttributes().removeAttribute("bike_speed_factor");
					link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, speedFactor);
				});

		// add link speed infrastructure factor of 0.5 for each bike link which doesn't have infrastructure attribute
		scenario.getNetwork().getLinks().values().parallelStream()
				.filter(link -> link.getAllowedModes().contains(TransportMode.bike))
				.filter(link -> link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR) == null)
				.forEach(link -> link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5));

		// add mode vehicles from here, since I messed this up a thousand times already
		var factory = scenario.getVehicles().getFactory();
		scenario.getVehicles().addVehicleType(createVehicleType(TransportMode.car, 7.5, 36.111111, 1.0, factory));
		scenario.getVehicles().addVehicleType(createVehicleType(TransportMode.ride, 7.5, 36.111111, 0.1, factory));

		// use twice the speed of 3.42, so that max speed ~25km/h on bike links and ~12km/h on regular streets with speed-factor of 0.5
		scenario.getVehicles().addVehicleType(createVehicleType(TransportMode.bike, 2.0, 6.84, 0.1, factory));
		return scenario;
	}

	static Config loadConfig(String[] args, ConfigGroup... customModules) {

		BicycleConfigGroup bikeConfigGroup = new BicycleConfigGroup();
		bikeConfigGroup.setBicycleMode(TransportMode.bike);

		//this feels a little messy, but I guess this is how var-args work
		List<ConfigGroup> moduleList = new ArrayList<>(Arrays.asList(customModules));
		moduleList.add(bikeConfigGroup);
		var moduleArray = moduleList.toArray(new ConfigGroup[0]);

		var config = ConfigUtils.loadConfig(args, moduleArray);

		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setUsePersonIdForMissingVehicleId(false);

		final long minDuration = 600;
		final long maxDuration = 3600 * 27;
		final long difference = 600;
		NEMOUtils.createTypicalDurations("home", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
		NEMOUtils.createTypicalDurations("work", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
		NEMOUtils.createTypicalDurations("education", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
		NEMOUtils.createTypicalDurations("leisure", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
		NEMOUtils.createTypicalDurations("shopping", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
		NEMOUtils.createTypicalDurations("other", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));

		return config;
	}

	public void run(String[] args) {
		Config config = loadConfig(args);

		Scenario scenario = loadScenario(config);

		Controler controler = loadControler(scenario);
		controler.run();
	}

	private static VehicleType createVehicleType(String id, double length, double maxV, double pce, VehiclesFactory factory) {
		var vehicleType = factory.createVehicleType(Id.create(id, VehicleType.class));
		vehicleType.setNetworkMode(id);
		vehicleType.setPcuEquivalents(pce);
		vehicleType.setLength(length);
		vehicleType.setMaximumVelocity(maxV);
		vehicleType.setWidth(1.0);
		return vehicleType;
	}
}
