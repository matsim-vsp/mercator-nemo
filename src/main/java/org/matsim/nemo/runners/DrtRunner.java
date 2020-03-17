package org.matsim.nemo.runners;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DrtRunner {

    private static final String shapeFileOption = "shapeFile";
    private static final String drtServiceAreaAttribute = "drtServiceArea";

    private static final Logger logger = Logger.getLogger(DrtRunner.class);

    public static void main(String[] args) throws CommandLine.ConfigurationException, URISyntaxException {

        var commandLine = new CommandLine.Builder(args)
                .allowPositionalArguments(true)
                .allowOptions(shapeFileOption)
                .requireOptions(shapeFileOption)
                .build();

        // -------------------------------- Config ----------------------------------
        var config = BaseCaseRunner.loadConfig(args, new DvrpConfigGroup(),
                new MultiModeDrtConfigGroup(), new DrtFaresConfigGroup(), new SwissRailRaptorConfigGroup(), new DrtSpeedUpConfigGroup());

        config.qsim().setNumberOfThreads(1); //drt works single threaded

        //set up drt config
        DrtConfigs.adjustDrtConfig(DrtConfigGroup.getSingleModeDrtConfig(config), config.planCalcScore(), config.plansCalcRoute());

        DrtSpeedUpModule.adjustConfig(config);

        // ------------------------------- Scenario ----------------------------------
        var scenario = BaseCaseRunner.loadScenario(config);

        logger.info("creating drt service area. Start reading in ruhr shape file");
        var shapePath = Paths.get(config.getContext().toURI()).getParent().resolve(commandLine.getOptionStrict(shapeFileOption));
        var serviceArea = ShapeFileReader.getAllFeatures(shapePath.toString()).stream()
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toList());

        logger.info("apply service area marker to all links");
        addDrtModeAndMarkServiceArea(scenario.getNetwork(), DrtConfigGroup.getSingleModeDrtConfig(config).getMode());
        tagTransitStopsInServiceArea(serviceArea, scenario.getTransitSchedule());

        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // -------------------------------- Controler -----------------------------------

        var controler = BaseCaseRunner.loadControler(scenario);

        logger.info("Start configuring drt. Add DrtModeModule, DvrpModule, DrtFareModule and activate drt-mode");
        DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(scenario.getConfig());
        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DrtFareModule());
        controler.addOverridingModule(new DrtSpeedUpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateModes(drtConfigGroup.getMode()));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(RaptorIntermodalAccessEgress.class).to(NemoRaptorIntermodalAccessEgress.class);
            }
        });

        controler.run();
    }

    private static void addDrtModeAndMarkServiceArea(Network network, String taxiNetworkMode) {

        logger.info("Add taxi mode to allowed modes where car and ride is allowed. If in service area add service area attribute to link");
        network.getLinks().values().parallelStream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car) && link.getAllowedModes().contains(TransportMode.ride))
                .forEach(link -> {

                    // copy all previous modes and add taxiNetworkMode
                    Set<String> modes = link.getAllowedModes();
                    Set<String> newModes = new HashSet<>(modes);
                    newModes.add(taxiNetworkMode);
                    link.setAllowedModes(newModes);
                });
        logger.info("Added drt mode to allowed modes and marked links with service area attribute");
    }

    private static void tagTransitStopsInServiceArea(List<Geometry> area, TransitSchedule schedule) {

        schedule.getFacilities().values().parallelStream()
                .filter(facility -> isInGeometry(facility.getCoord(), area))
                .forEach(facility -> facility.getAttributes().putAttribute("drt-stop", "true"));
    }

    private static boolean isInGeometry(Coord coord, Collection<org.locationtech.jts.geom.Geometry> geometry) {

        Point point = MGC.coord2Point(coord);
        return geometry.stream().anyMatch(geometry1 -> geometry1.contains(point));
    }
}
