package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
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
				id -> scenario.getPopulation().getPersons().get(id).getAttributes().getAttribute("was_moved") != null && scenario.getPopulation().getPersons().get(id).getAttributes().getAttribute("moved-all-activities") != null && agentsFilter.includeAgent(id) :
				agentsFilter::includeAgent;

		parseEventsFile(Paths.get(eventFile), Paths.get(outputFile), includePerson);
	}

	private void parseEventsFile(Path file, Path output, Predicate<Id<Person>> includePerson) throws IOException {

		EventsManager manager = EventsUtils.createEventsManager();
		TripEventHandler handler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), includePerson);
		manager.addHandler(handler);
		new MatsimEventsReader(manager).readFile(file.toString());

		logger.info("Writing files to: " + output.toString());

		try (Writer writer = Files.newBufferedWriter(output)) {
			try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
				printer.printRecord("personId", "tripNo", "fromX", "fromY", "toX", "toY", "startTime", "endTime", "distance", "mainMode");

				for (Map.Entry<Id<Person>, List<TripEventHandler.Trip>> tripWithId : handler.getTrips().entrySet()) {

					for (int i = 0; i < tripWithId.getValue().size(); i++) {

						var trip = tripWithId.getValue().get(i);
						// this should take facilities for accurate results, but we don't have those at this place, so go for links intsead for now
						Coord departureCoord = network.getLinks().get(trip.getDepartureLink()).getToNode().getCoord();
						Coord arrivalCoord = network.getLinks().get(trip.getArrivalLink()).getToNode().getCoord();
						double distance = CoordUtils.calcEuclideanDistance(departureCoord, arrivalCoord);
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

	private double calculateBeelineDistance(TripEventHandler.Trip trip) {

		ActivityFacility departureFacility = scenario.getActivityFacilities().getFacilities().get(trip.getDepartureFacility());
		ActivityFacility arrivalFacility = scenario.getActivityFacilities().getFacilities().get(trip.getArrivalFacility());
		return CoordUtils.calcEuclideanDistance(departureFacility.getCoord(), arrivalFacility.getCoord());
	}
}
