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

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunRuhrgebietScenario;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

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

    private static final Logger log = Logger.getLogger(RunRuhrgebietSmartCityScenario.class);
    private static final String drtServiceAreaAttribute = "drtServiceArea";

    public static void main(String[] args) {

        if (args.length != 2)
            throw new RuntimeException("two arguments are required: path/to/config path/to/service/area/shape/file");

        String configFile = args[0]; // path to config file
        Path serviceAreaShapeFile = Paths.get(args[1]); // path to shape file
        RunRuhrgebietScenario ruhrgebiet = new RunRuhrgebietScenario(new String[]{"--config-path", configFile});

        run(ruhrgebiet, serviceAreaShapeFile);
    }

    private static void run(RunRuhrgebietScenario ruhrgebiet, Path serviceAreaShape) {

        Controler controler = ruhrgebiet.prepareControler();

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
        for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
            xy2Links.run(p);
        }

        controler.run();
        log.info("Done.");
    }
}
