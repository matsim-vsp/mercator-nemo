package org.matsim.nemo.runners;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
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
                new MultiModeDrtConfigGroup(), new DrtFaresConfigGroup());

        config.qsim().setNumberOfThreads(1); //drt works single threaded

        //set up drt config
        DrtConfigs.adjustDrtConfig(DrtConfigGroup.getSingleModeDrtConfig(config), config.planCalcScore(), config.plansCalcRoute());

        // ------------------------------- Scenario ----------------------------------
        var scenario = BaseCaseRunner.loadScenario(config);

        logger.info("creating drt service area. Start reading in ruhr shape file");
        var shapePath = Paths.get(config.getContext().toURI()).getParent().resolve(commandLine.getOptionStrict(shapeFileOption));
        var serviceArea = ShapeFileReader.getAllFeatures(shapePath.toString()).stream()
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toList());

        logger.info("apply service area marker to all links");
        addDrtModeAndMarkServiceArea(scenario.getNetwork(), serviceArea, DrtConfigGroup.getSingleModeDrtConfig(config).getMode(), drtServiceAreaAttribute);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // -------------------------------- Controler -----------------------------------

        var controler = BaseCaseRunner.loadControler(scenario);

        logger.info("Start configuring drt. Add DrtModeModule, DvrpModule, DrtFareModule and activate drt-mode");
        DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(scenario.getConfig());
        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DrtFareModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateModes(drtConfigGroup.getMode()));

        controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtConfigGroup.getMode()) {
            @Override
            protected void configureQSim() {
                bindModal(PassengerRequestValidator.class).toInstance(new DrtRunner.ServiceAreaRequestValidator(drtServiceAreaAttribute));
            }
        });

        controler.run();
    }

    private static void addDrtModeAndMarkServiceArea(Network network, Collection<org.locationtech.jts.geom.Geometry> serviceArea, String taxiNetworkMode, String serviceAreaAttribute) {

        logger.info("Add taxi mode to allowed modes where car and ride is allowed. If in service area add service area attribute to link");
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
        logger.info("Added drt mode to allowed modes and marked links with service area attribute");
    }

    private static void addServiceAreaAttribute(Link link, Collection<org.locationtech.jts.geom.Geometry> serviceArea, String attributeKey) {

        if (isInGeometry(link.getCoord(), serviceArea))
            link.getAttributes().putAttribute(attributeKey, true);
        else
            link.getAttributes().putAttribute(attributeKey, false);
    }

    private static boolean isInGeometry(Coord coord, Collection<org.locationtech.jts.geom.Geometry> geometry) {

        Point point = MGC.coord2Point(coord);
        return geometry.stream().anyMatch(geometry1 -> geometry1.contains(point));
    }

    private static final class ServiceAreaRequestValidator implements PassengerRequestValidator {

        private static final String FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE = "from_link_not_in_service_area";
        private static final String TO_LINK_NOT_IN_SERVICE_AREA_CAUSE = "to_link_not_in_service_area";

        private final DefaultPassengerRequestValidator defaultValidator = new DefaultPassengerRequestValidator();

        private final String serviceAreaAttribute;

        private ServiceAreaRequestValidator(String serviceAreaAttribute) {
            this.serviceAreaAttribute = serviceAreaAttribute;
        }

        @Override
        public Set<String> validateRequest(PassengerRequest request) {

            // the returned set is immutable
            Set<String> invalidRequestCauses = new HashSet<>(defaultValidator.validateRequest(request));

            boolean fromLinkInServiceArea = (boolean) request.getFromLink()
                    .getAttributes()
                    .getAttribute(serviceAreaAttribute);
            boolean toLinkInServiceArea = (boolean) request.getToLink().getAttributes().getAttribute(serviceAreaAttribute);

            if (!fromLinkInServiceArea) {
                invalidRequestCauses.add(FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE);
            }
            if (!toLinkInServiceArea) {
                invalidRequestCauses.add(TO_LINK_NOT_IN_SERVICE_AREA_CAUSE);
            }

            return invalidRequestCauses;
        }
    }
}
