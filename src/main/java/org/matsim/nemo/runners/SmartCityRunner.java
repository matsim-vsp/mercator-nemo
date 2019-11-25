package org.matsim.nemo.runners;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.Bicycles;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.nemo.runners.smartCity.ServiceAreaRequestValidator;
import org.matsim.nemo.runners.smartCity.SmartCityNetworkModification;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

public class SmartCityRunner {

    private static final Logger logger = Logger.getLogger(SmartCityRunner.class);
	private static final String drtServiceAreaAttribute = "drtServiceArea";

	@Parameter(names = "-inputDir", required = true)
	private String inputDir;

	@Parameter(names = "-outputDir", required = true)
	private String outputDir;

	@Parameter(names = "-runId")
	private String runId;

	@Parameter(names = "-reduced")
	private boolean reducedDebugScenario = false;

	public static void main(String[] args) {

		SmartCityRunner runner = new SmartCityRunner();
		JCommander.newBuilder().addObject(runner).build().parse(args);
		runner.run();
	}

	private static void addTypicalDurations(Config config, String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds) {

		for (long duration = minDurationInSeconds; duration <= maxDurationInSeconds; duration += durationDifferenceInSeconds) {
			final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(type + "_" + duration + ".0");
			params.setTypicalDuration(duration);
			config.planCalcScore().addActivityParams(params);
		}
	}

	private void run() {

		Config config = loadConfig(Paths.get(inputDir), Paths.get(outputDir), runId);

		Scenario scenario = loadScenario(config);

		Controler controler = createControler(scenario);

        logger.info("Start SmartCity run");
        controler.run();

	}

	private Controler createControler(Scenario scenario) {

        logger.info("Start creating controler");
		Controler controler = new Controler(scenario);

        logger.info("add raptor pt-router");
		controler.addOverridingModule(new SwissRailRaptorModule());

		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

        logger.info("Start configuring drt. Add DrtModeModule, DvrpModule, DrtFareModule and activate drt-mode");
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(scenario.getConfig());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DrtFareModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(drtConfigGroup.getMode()));

        logger.info("Set passengerRequestValidator");
		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtConfigGroup.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class).toInstance(new ServiceAreaRequestValidator(drtServiceAreaAttribute));
			}
		});

        logger.info("Add bicycle contrib");
		Bicycles.addAsOverridingModule(controler);

        logger.info("Done configuring controler");
		return controler;
	}

	private Scenario loadScenario(Config config) {

        logger.info("start loading scenario");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// for debugging purposes remove 90% of the population
        if (reducedDebugScenario) {
            logger.info("running in reduced mode. Removing 90% of the population. Original size is: " + scenario.getPopulation().getPersons().size());
            scenario.getPopulation().getPersons().entrySet().removeIf(person -> Math.random() > 0.1);
            logger.info("reduced population. New size is: " + scenario.getPopulation().getPersons().size());
        }

        logger.info("creating drt service area. Start reading in ruhr shape file");
		Collection<Geometry> serviceArea = ShapeFileReader.getAllFeatures(Paths.get(inputDir).resolve("ruhrgebiet_boundary.shp").toString()).stream()
				.map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
				.collect(Collectors.toList());
        logger.info("apply service area marker to all links");
		SmartCityNetworkModification.addDrtModeAndMarkServiceArea(scenario.getNetwork(), serviceArea, DrtConfigGroup.getSingleModeDrtConfig(config).getMode(), drtServiceAreaAttribute);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		// replace former access- egress_walk with non_network_walk
        logger.info("replace former access_walk and egress_walk modes with non_network_mode");
		scenario.getPopulation().getPersons().values().parallelStream()
				.flatMap(person -> person.getPlans().stream())
				.flatMap(plan -> plan.getPlanElements().stream())
				.filter(element -> element instanceof Leg)
				.map(element -> (Leg) element)
				.filter(leg -> leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk"))
				.forEach(leg -> leg.setMode(TransportMode.non_network_walk));

        logger.info("finished loading scenario");
		return scenario;
	}

	private Config loadConfig(Path inputDir, Path outputDir, String runId) {

        Config config = ConfigUtils.loadConfig(inputDir.resolve("config.xml").toString(), new DvrpConfigGroup(),
				new MultiModeDrtConfigGroup(), new DrtFaresConfigGroup(), new BicycleConfigGroup());

		config.controler().setOutputDirectory(outputDir.toString());
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		if (runId != null) config.controler().setRunId(runId);

		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		// remove bike and ride from teleported modes
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setNumberOfThreads(1); //drt works only single threaded
        config.qsim().setUsePersonIdForMissingVehicleId(false);

        //set up drt config
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.getSingleModeDrtConfig(config), config.planCalcScore(), config.plansCalcRoute());

		final long minDuration = 600;
		final long maxDuration = 3600 * 27;
		final long difference = 600;
		addTypicalDurations(config, "home", minDuration, maxDuration, difference);
		addTypicalDurations(config, "work", minDuration, maxDuration, difference);
		addTypicalDurations(config, "education", minDuration, maxDuration, difference);
		addTypicalDurations(config, "leisure", minDuration, maxDuration, difference);
		addTypicalDurations(config, "shopping", minDuration, maxDuration, difference);
		addTypicalDurations(config, "other", minDuration, maxDuration, difference);

        // fail fast if config is inconsistent
        //config.checkConsistency();
		return config;
	}
}
