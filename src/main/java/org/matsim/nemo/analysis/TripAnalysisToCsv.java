package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.nemo.RuhrAgentsFilter;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TripAnalysisToCsv {

	private static final Logger logger = Logger.getLogger(TripAnalysisToCsv.class);
	private static final String MOVED_ALL_ACTIVITIES = "moved_all_activities";
	private static final String WAS_MOVED_KEY = "was_moved";

	@Parameter(names = {"-eventFile", "-ef"}, required = true)
	private String eventFile = "";

	@Parameter(names = {"-networkFile", "-nf"}, required = true)
	private String networkFile = "";

	@Parameter(names = {"-populationFile", "-pf"}, required = true)
	private String populationFile = "";

	@Parameter(names = {"-scalingFactor", "-sf"})
	private double scalingFactor = 100;

	@Parameter(names = {"-ruhrShape", "-rs"})
	private String ruhrShapeFile;

	@Parameter(names = {"-outputFile", "-of"})
	private String outputFile;

	@Parameter(names = {"onlyMovedAgents", "-om"})
	private boolean onlyMovedAgentsByMurmo = false;

	private Network network;
	private Scenario scenario;

	public static void main(String[] args) throws IOException {

		TripAnalysisToCsv analysis = new TripAnalysisToCsv();
		JCommander.newBuilder().addObject(analysis).build().parse(args);
		analysis.run();
	}

	private void run() throws IOException {

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);
		RuhrAgentsFilter agentsFilter = new RuhrAgentsFilter(this.scenario, ShapeFileReader.getAllFeatures(this.ruhrShapeFile));

		network = NetworkUtils.readNetwork(networkFile);

		if (onlyMovedAgentsByMurmo) logger.info("Only counting moved agents.");

		Predicate<Id<Person>> includePerson = onlyMovedAgentsByMurmo ?
				id -> scenario.getPopulation().getPersons().get(id).getAttributes().getAttribute(WAS_MOVED_KEY) != null && agentsFilter.includeAgent(id) :
				agentsFilter::includeAgent;

		parseEventsFile(Paths.get(eventFile), Paths.get(outputFile), includePerson);
	}

	private void parseEventsFile(Path file, Path output, Predicate<Id<Person>> includePerson) throws IOException {

		EventsManager manager = EventsUtils.createEventsManager();
		TripEventHandler handler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), includePerson);
		manager.addHandler(handler);
		new MatsimEventsReader(manager).readFile(file.toString());

		var testPerson = handler.getTrips().get(Id.createPersonId("1016010401"));

		logger.info("Writing files to: " + output.toString());

		try (Writer writer = Files.newBufferedWriter(output)) {
			try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
				printer.printRecord("personId", "tripNo", "fromX", "fromY", "toX", "toY", "startTime", "endTime", "distance", "mainMode");

				for (Map.Entry<Id<Person>, List<TripEventHandler.Trip>> tripWithId : handler.getTrips().entrySet()) {

					var person = scenario.getPopulation().getPersons().get(tripWithId.getKey());
					var selectedPlan = person.getSelectedPlan();


					for (int i = 0; i < tripWithId.getValue().size(); i++) {


						var trip = tripWithId.getValue().get(i);

						var departureCoord = getCoord(selectedPlan, trip.getDepartureFacility(), trip.getDepartureLink());
						var arrivalCoord = getCoord(selectedPlan, trip.getArrivalFacility(), trip.getArrivalLink());
						var distance = CoordUtils.calcEuclideanDistance(departureCoord, arrivalCoord);

						printer.printRecord(
								tripWithId.getKey().toString(), // person id
								i, // trip index in combination with person id results in distinct index. This is important for joining results from different runs
								departureCoord.getX(),
								departureCoord.getY(),
								arrivalCoord.getX(),
								arrivalCoord.getY(),
								trip.getDepartureTime(),
								trip.getArrivalTime(),
								distance,
								trip.getMainMode()
						);
					}
				}
				printer.flush();
			}
		}
	}

	/**
	 * 1. Tries to get the coordinate from the scenario's facility which has the supplied facilityId
	 * 2. If the facility can't be found within the scenario the activity from the plan with the corresponding facilityId
	 * is retrieved and the coordinate of the activity is returned
	 * 3. If no activity is found or no coordinate is set on the activity the coordinate of the supplied link is taken
	 * from the network as a fallback - this is less accurate than the facility or activity id
	 */
	private Coord getCoord(Plan plan, Id<ActivityFacility> facilityId, Id<Link> linkId) {

		if (scenario.getActivityFacilities().getFacilities().size() > 0 || scenario.getActivityFacilities().getFacilities().containsKey(facilityId)) {
			return scenario.getActivityFacilities().getFacilities().get(facilityId).getCoord();
		}

		var optionalActivity = plan.getPlanElements().stream()
				.filter(element -> element instanceof Activity)
				.map(element -> (Activity) element)
				.filter(activity -> !StageActivityTypeIdentifier.isStageActivity(activity.getType()))
				.filter(activity -> activity.getFacilityId() != null)
				.filter(activity -> activity.getFacilityId().equals(facilityId))
				.findAny();

		if (optionalActivity.isPresent() && optionalActivity.get().getCoord() != null) {
			return optionalActivity.get().getCoord();
		}

		logger.warn("Falling back to link coord. This is not as accurate as Facility or Activity coordinates");
		return network.getLinks().get(linkId).getCoord();
	}
}
