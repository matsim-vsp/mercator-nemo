package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.nemo.RuhrAgentsFilter;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;
import org.matsim.nemo.util.ExpectedDistanceDistribution;
import org.matsim.nemo.util.ExpectedModalDistanceDistribution;
import org.matsim.nemo.util.ExpectedModalShare;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModalDistanceAnalysis {

	private static Logger logger = Logger.getLogger(ModalDistanceAnalysis.class);

	@Parameter(names = {"-eventFile", "-ef"}, required = true)
	private List<String> eventFiles = new ArrayList<>();

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

	private Scenario scenario;
	private Network network;

	public static void main(String[] args) {

		ModalDistanceAnalysis analysis = new ModalDistanceAnalysis();
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

		List<Tuple<String, TripAnalysis>> result = eventFiles.parallelStream()
				.map(eventFile -> parseEventFile(Paths.get(eventFile), expectedDistanceDistribution, agentsFilter))
				.collect(Collectors.toList());


		new TripAnalysisExcelWriter.Builder()
				.filePath(Paths.get(outputFile))
				.scalingFactor(scalingFactor)
				.tripAnalysises(result)
				.addExpectedModalShare(ExpectedModalShare.create())
				.addExpectedDistanceDistribution(ExpectedDistanceDistribution.create())
				.addExpectedModalDistanceDistribution(ExpectedModalDistanceDistribution.create())
				.build()
				.write();
	}

	private Tuple<String, TripAnalysis> parseEventFile(Path file, DistanceDistribution expectedDistribution, RuhrAgentsFilter agentsFilter) {

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

		TripAnalysis analysis = new TripAnalysis(tripEventHandler.getTrips(), scenario, network);
		String runId = file.getFileName().toString().split("[.]")[0];

		if (tripEventHandler.getStuckPersons().size() > 0) {
			logger.warn("Run: " + runId + " had " + tripEventHandler.getStuckPersons().size() + " stuck agents.");
		}

		return Tuple.of(runId, analysis);
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
}
