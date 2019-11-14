package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.nemo.RuhrAgentsFilter;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;
import org.matsim.nemo.util.ExpectedModalDistanceDistribution;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpectedAndExperiencedTripsComparison {

    private static Logger logger = Logger.getLogger(ExpectedAndExperiencedTripsComparison.class);

    @Parameter(names = {"-eventFile", "-ef"}, required = true)
    private String eventFile = "";

    @Parameter(names = {"-networkFile", "-nf"}, required = true)
    private String networkFile = "";

    @Parameter(names = {"-populationFile", "-pf"}, required = true)
    private String populationFile = "";

    @Parameter(names = {"-ruhrShape", "-rs"})
    private String ruhrShapeFile = "";

    private Scenario scenario;
    private Network network;

    public static void main(String[] args) {

        ExpectedAndExperiencedTripsComparison analysis = new ExpectedAndExperiencedTripsComparison();
        JCommander.newBuilder().addObject(analysis).build().parse(args);
        analysis.run();
    }

    private void run() {

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        RuhrAgentsFilter agentsFilter = new RuhrAgentsFilter(this.scenario, ShapeFileReader.getAllFeatures(this.ruhrShapeFile));


        network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);

        DistanceDistribution expectedDistanceDistribution = ExpectedModalDistanceDistribution.create();

        EventsManager manager = EventsUtils.createEventsManager();

        TripEventHandler tripEventHandler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), agentsFilter::includeAgent);

        manager.addHandler(tripEventHandler);
        new MatsimEventsReader(manager).readFile(Paths.get(eventFile).toString());

        Map<Id<Person>, List<TripStructureUtils.Trip>> expectedTrips = new HashMap<>();

        for (Person value : scenario.getPopulation().getPersons().values()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(value.getSelectedPlan());
            expectedTrips.put(value.getId(), trips);
        }

        int personsWithinShape = 0;
        Map<Id<Person>, Person> excluded = new HashMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (agentsFilter.includeAgent(person.getId())) personsWithinShape++;
            else excluded.put(person.getId(), person);
        }

        // assert that the correct amount of trips was counted
        Map<Id<Person>, List<TripEventHandler.Trip>> observedTrips = tripEventHandler.getTrips();
        List<TripEventHandler.Trip> flattenedObservedTrips = observedTrips.values().stream().flatMap(Collection::stream).collect(Collectors.toList());



        for (Map.Entry<Id<Person>, List<TripEventHandler.Trip>> idListEntry : tripEventHandler.getTrips().entrySet()) {
            List<TripStructureUtils.Trip> tripsFromPlan = expectedTrips.get(idListEntry.getKey());
            assert idListEntry.getValue().size() == tripsFromPlan.size();
        }
    }

    private NamedDistanceDistribution parseEventFile(Path file, DistanceDistribution expectedDistribution, RuhrAgentsFilter agentsFilter) {

        EventsManager manager = EventsUtils.createEventsManager();

        TripEventHandler tripEventHandler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), agentsFilter::includeAgent);

        manager.addHandler(tripEventHandler);
        new MatsimEventsReader(manager).readFile(file.toString());

        DistanceDistribution simulatedDistribution = expectedDistribution.copyWithEmptyBins();

        tripEventHandler.getTrips().entrySet().parallelStream().flatMap(entry -> entry.getValue().stream())
                .forEach(trip -> {
                    double distance = calculateBeelineDistance(trip);
                    simulatedDistribution.increaseCountByOne(trip.getMainMode(), distance);
                });

        String runId = file.getFileName().toString().split("[.]")[0];

        if (tripEventHandler.getStuckPersons().size() > 0) {
            logger.warn("Run: " + runId + " had " + tripEventHandler.getStuckPersons().size() + " stuck agents.");
        }

        return new NamedDistanceDistribution(runId, simulatedDistribution);
    }

    private double calculateBeelineDistance(TripEventHandler.Trip trip) {

        if (scenario.getActivityFacilities().getFacilities().containsKey(trip.getDepartureFacility()) ||
                scenario.getActivityFacilities().getFacilities().containsKey(trip.getArrivalFacility())
        ) {
            ActivityFacility departureFacility = scenario.getActivityFacilities().getFacilities().get(trip.getDepartureFacility());
            ActivityFacility arrivalFacility = scenario.getActivityFacilities().getFacilities().get(trip.getArrivalFacility());
            return CoordUtils.calcEuclideanDistance(departureFacility.getCoord(), arrivalFacility.getCoord());
        } else {
            Link departureLink = network.getLinks().get(trip.getDepartureLink());
            Link arrivalLink = network.getLinks().get(trip.getArrivalLink());
            return CoordUtils.calcEuclideanDistance(departureLink.getToNode().getCoord(), arrivalLink.getToNode().getCoord());
        }
    }

    private static class NamedDistanceDistribution {
        private String name;
        private DistanceDistribution distanceDistribution;

        private NamedDistanceDistribution(String name, DistanceDistribution distanceDistribution) {
            this.name = name;
            this.distanceDistribution = distanceDistribution;
        }
    }
}
