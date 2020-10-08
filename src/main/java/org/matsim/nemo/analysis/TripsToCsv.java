package org.matsim.nemo.analysis;

import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;

import java.util.Collections;
import java.util.List;

public class TripsToCsv {

    // will only run once and I'm to lazy to write up a class
    private static final IdMap<Person, Plan> agentRecordsWasMoved = new IdMap<>(Person.class);
    private static final IdMap<Person, Plan> agentRecordsWasMovedAllActivities = new IdMap<>(Person.class);

    public static void main(String[] args) {

        // var networkPath = "C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct\\deurbanisation-50pct-matches.output_network.xml.gz";
        // var plansPath = "C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct\\deurbanisation-50pct-matches.output_plans.xml.gz";
        var eventsFile = "C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct\\deurbanisation-50pct-matches.output_events.xml.gz";

    /*    var network = NetworkUtils.readNetwork(networkPath);
        var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        scenario.setNetwork(network);
        scenario.setPopulation(PopulationUtils.readPopulation(plansPath));

     */

        var config = ConfigUtils.loadConfig("C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct\\deurbanisation-50pct-matches.output_config.xml");
        config.plans().setInputFile("deurbanisation-50pct-matches.output_plans.xml.gz");

        var scenario = ScenarioUtils.loadScenario(config);
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (person.getAttributes().getAttribute("was_moved") != null) {
                if (person.getAttributes().getAttribute("moved_all_activities") != null) {
                    agentRecordsWasMovedAllActivities.put(person.getId(), PopulationUtils.createPlan());
                } else {
                    agentRecordsWasMoved.put(person.getId(), PopulationUtils.createPlan());
                }
            }
        }

        var eventsToActivities = new EventsToActivities();
        eventsToActivities.addActivityHandler(TripsToCsv::handleActivity);
        var eventsToLegs = new EventsToLegs(scenario);
        eventsToLegs.addLegHandler(TripsToCsv::handleLeg);

        var manager = EventsUtils.createEventsManager();
        manager.addHandler(eventsToActivities);
        manager.addHandler(eventsToLegs);
        manager.initProcessing();
        EventsUtils.readEvents(manager, eventsFile);

        var writer = new TripsAndLegsCSVWriter(scenario, new NoTripWriterExtension(), new NoLegsWriterExtension());
        writer.write(agentRecordsWasMovedAllActivities, "trips.movedAll.csv.gz", "legs.movedAll.csv.gz");
        writer.write(agentRecordsWasMoved, "trips.movedHome.csv.gz", "legs.movedHome.csv.gz");
    }

    static synchronized void handleActivity(PersonExperiencedActivity o) {
        // Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
        // on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
        Id<Person> agentId = o.getAgentId();
        Activity activity = o.getActivity();
        Plan plan = agentRecordsWasMoved.get(agentId);
        if (plan != null) {
            agentRecordsWasMoved.get(agentId).addActivity(activity);
        }

        Plan planAllActivities = agentRecordsWasMovedAllActivities.get(agentId);
        if (planAllActivities != null) {
            agentRecordsWasMovedAllActivities.get(agentId).addActivity(activity);
        }
    }

    static synchronized void handleLeg(PersonExperiencedLeg o) {
        // Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
        // on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
        Id<Person> agentId = o.getAgentId();
        Leg leg = o.getLeg();
        Plan plan = agentRecordsWasMoved.get(agentId);
        if (plan != null) {
            agentRecordsWasMoved.get(agentId).addLeg(leg);
        }
        Plan planAllActivities = agentRecordsWasMovedAllActivities.get(agentId);
        if (planAllActivities != null) {
            agentRecordsWasMovedAllActivities.get(agentId).addLeg(leg);
        }
    }

    static class NoTripWriterExtension implements TripsAndLegsCSVWriter.CustomTripsWriterExtension {
        @Override
        public String[] getAdditionalTripHeader() {
            return new String[0];
        }

        @Override
        public List<String> getAdditionalTripColumns(TripStructureUtils.Trip trip) {
            return Collections.EMPTY_LIST;
        }
    }

    static class NoLegsWriterExtension implements TripsAndLegsCSVWriter.CustomLegsWriterExtension {
        @Override
        public String[] getAdditionalLegHeader() {
            return new String[0];
        }

        @Override
        public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
            return Collections.EMPTY_LIST;
        }
    }
}
