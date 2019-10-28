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

package org.matsim.nemo.runners.smartCity;

/**
 * This class starts a simulation run with DRT.
 * <p>
 * The DRT service area is set to the the Ruhrgebiet area (see input shape
 * file). - Initial plans are not modified.
 * </p>
 *
 * @author jlaudan
 */

public final class RunRuhrgebietSmartCityScenario {

	//TODO If one wants to simulate this scenario have a look at the matsim-berlin scenario for the configuration of a drt set up
	/*

    private static final Logger log = Logger.getLogger(RunRuhrgebietSmartCityScenario.class);
    private static final String drtServiceAreaAttribute = "drtServiceArea";
	private static final RunType runType = RunType.onePercent;

	public static void main(String[] args) {

        if (args.length != 2)
            throw new RuntimeException("two arguments are required: path/to/config path/to/service/area/shape/file");

        String configFile = args[0]; // path to config file
        Path serviceAreaShapeFile = Paths.get(args[1]); // path to shape file

		run(configFile, serviceAreaShapeFile);
    }

	private static void run(String configFileName, Path serviceAreaShape) {

		Config config = ConfigUtils.loadConfig(configFileName);

		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

		final long minDuration = 600;
		final long maxDuration = 3600 * 27;
		final long difference = 600;
		addTypicalDurations(config, "home", minDuration, maxDuration, difference);
		addTypicalDurations(config, "work", minDuration, maxDuration, difference);
		addTypicalDurations(config, "education", minDuration, maxDuration, difference);
		addTypicalDurations(config, "leisure", minDuration, maxDuration, difference);
		addTypicalDurations(config, "shopping", minDuration, maxDuration, difference);
		addTypicalDurations(config, "other", minDuration, maxDuration, difference);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (runType == RunType.reduced) {
			scenario.getPopulation().getPersons().entrySet().removeIf(person -> Math.random() < 0.1);
		}

		Controler controler = new Controler(scenario);

		// add raptor router
		controler.addOverridingModule(new SwissRailRaptorModule());

		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

		// set different speed levels for bike highways
		controler.addOverridingQSimModule(new AbstractQSimModule() {

			@Override
			protected void configureQSim() {
				bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
					@Inject
					private EventsManager events;

					@Override
					public QNetworkFactory get() {
						final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
						factory.setLinkSpeedCalculator(new BikeLinkSpeedCalculator());
						return factory;
					}
				});
			}
		});

		controler.getConfig().controler().setWritePlansUntilIteration(5);
		PlansCalcRouteConfigGroup pcr = controler.getConfig().plansCalcRoute();

		pcr.addModeRoutingParams(new PlansCalcRouteConfigGroup.ModeRoutingParams("access_walk")
				.setTeleportedModeSpeed(pcr.getModeRoutingParams().get(TransportMode.walk).getTeleportedModeSpeed())
				.setBeelineDistanceFactor(pcr.getModeRoutingParams().get(TransportMode.walk).getBeelineDistanceFactor())
		);

		pcr.addModeRoutingParams(new PlansCalcRouteConfigGroup.ModeRoutingParams("egress_walk")
				.setTeleportedModeSpeed(pcr.getModeRoutingParams().get(TransportMode.walk).getTeleportedModeSpeed())
				.setBeelineDistanceFactor(pcr.getModeRoutingParams().get(TransportMode.walk).getBeelineDistanceFactor())
		);

		PlanCalcScoreConfigGroup scoring = controler.getConfig().planCalcScore();
		scoring.addModeParams(new PlanCalcScoreConfigGroup.ModeParams("access_walk")
				.setMarginalUtilityOfTraveling(scoring.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling()));

		scoring.addModeParams(new PlanCalcScoreConfigGroup.ModeParams("egress_walk")
				.setMarginalUtilityOfTraveling(scoring.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling()));

		final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("drt interaction");
		params.setTypicalDuration(0);
		params.setScoringThisActivityAtAll(false);
		scoring.addActivityParams(params);

        // remove bike and ride from teleported modes
        controler.getConfig().plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
        controler.getConfig().plansCalcRoute().removeModeRoutingParams(TransportMode.ride);

        // add drt config groups
        controler.getConfig().addModule(new DvrpConfigGroup());
        controler.getConfig().addModule(new DrtConfigGroup());
        controler.getConfig().addModule(new DrtFaresConfigGroup());
        DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(controler.getConfig()), controler.getConfig().planCalcScore());

        // adjust qsim for drt
        controler.getConfig().qsim().setNumberOfThreads(1);
        controler.getConfig().qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

        // adjust route factory for drt
        controler.getScenario().getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // add drt modules to controler
        controler.addOverridingModule(new DrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new DrtFareModule());
        controler.configureQSimComponents(
                DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));

        // reject drt requests outside the service area
        controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(controler.getConfig()).getMode()) {
            @Override
            protected void configureQSim() {
                bindModal(PassengerRequestValidator.class)
                        .toInstance(new ServiceAreaRequestValidator(drtServiceAreaAttribute));
            }
        });

        controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MainModeIdentifier.class).to(InteractionMainModeIdentifier.class);
			}
		});

        // adjust the network, to contain drtServiceAreaAttribute used by above RequestValidator
        Collection<Geometry> serviceArea = ShapeFileReader.getAllFeatures(serviceAreaShape.toString()).stream()
                .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                .collect(Collectors.toSet());
        SmartCityNetworkModification.addSAVmode(controler.getScenario(), serviceArea, TransportMode.car, drtServiceAreaAttribute);

        // make people live on links that are car accessible, otherwise the drt module crashes
        NetworkFilterManager networkFilterManager = new NetworkFilterManager(controler.getScenario().getNetwork());
        networkFilterManager.addLinkFilter(l -> l.getAllowedModes().contains(TransportMode.car));
        Network network = networkFilterManager.applyFilters();
        XY2Links xy2Links = new XY2Links(network, controler.getScenario().getActivityFacilities());

		controler.getScenario().getPopulation().getPersons().values().parallelStream()
				// make people live on links that are car accessible, otherwise the drt module crashes
				.peek(xy2Links::run)
				.flatMap(person -> person.getPlans().parallelStream())
				.flatMap(plan -> plan.getPlanElements().parallelStream())
				// get all legs
				.filter(element -> element instanceof Leg)
				.map(element -> (Leg) element)
				.filter(leg -> leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk"))
				// change all access egress walks to non_network_walk, to reflect changes in the newest matsim version
				.forEach(leg -> leg.setMode(TransportMode.non_network_walk));

		/*controler.getScenario().getPopulation().getPersons().values().parallelStream()
				.flatMap(person -> ((Person) person).getPlans().stream())
				.flatMap(plan -> TripStructureUtils.getTrips(plan, new StageActivityTypesImpl(" interaction")).stream())
				.flatMap(trip -> TripStructureUtils.
*/
	/*
		controler.run();
        log.info("Done.");
    }

	private static void addTypicalDurations(Config config, String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds) {

		for (long duration = minDurationInSeconds; duration <= maxDurationInSeconds; duration += durationDifferenceInSeconds) {
			final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(type + "_" + duration + ".0");
			params.setTypicalDuration(duration);
			config.planCalcScore().addActivityParams(params);
		}
	}

	private enum RunType {onePercent, reduced}

	*/
}
