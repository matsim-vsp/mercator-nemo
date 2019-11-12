package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;
import org.matsim.nemo.util.ExpectedModalDistanceDistribution;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
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

	private Scenario scenario;
	private Network network;

	public static void main(String[] args) throws IOException {

		ModalDistanceAnalysis analysis = new ModalDistanceAnalysis();
		JCommander.newBuilder().addObject(analysis).build().parse(args);
		analysis.run();
	}

	private void run() throws IOException {

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);

		network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		DistanceDistribution expectedDistanceDistribution = ExpectedModalDistanceDistribution.create();

		List<NamedDistanceDistribution> result = eventFiles.parallelStream()
				.map(eventFile -> parseEventFile(Paths.get(eventFile), expectedDistanceDistribution))
				.collect(Collectors.toList());

		// write results to an excel file
		Workbook wb = new XSSFWorkbook();

		Sheet sheet = wb.createSheet("cadytsV3");
		Row titleRow = sheet.createRow(0);
		titleRow.createCell(0).setCellValue("mode");
		titleRow.createCell(1).setCellValue("lower limit");
		titleRow.createCell(2).setCellValue("upper limit");
		titleRow.createCell(3).setCellValue("expected count");

		int cellIndex = 4;
		for (NamedDistanceDistribution namedDistanceDistribution : result) {

			titleRow.createCell(cellIndex).setCellValue(namedDistanceDistribution.name);
			cellIndex++;
		}

		// now, write values of expected
		Collection<DistanceDistribution.DistanceBin> expectedBins = expectedDistanceDistribution.getDistanceBins().stream()
				.sorted(this::compareBinsByModeAndDistanceRange)
				.collect(Collectors.toList());

		int rowIndex = 1; // start with one, since we used 0 for header
		for (DistanceDistribution.DistanceBin expectedBin : expectedBins) {

			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(expectedBin.getMode());
			row.createCell(1).setCellValue(expectedBin.getDistanceRange().getLowerLimit());
			row.createCell(2).setCellValue(expectedBin.getDistanceRange().getUpperLimit());
			row.createCell(3).setCellValue(expectedBin.getValue());
			rowIndex++;
		}

		// now write values of simulated
		rowIndex = 1;
		cellIndex = 4;

		for (NamedDistanceDistribution namedDistanceDistribution : result) {

			List<DistanceDistribution.DistanceBin> bins = namedDistanceDistribution.distanceDistribution.getDistanceBins().stream()
					.sorted(this::compareBinsByModeAndDistanceRange)
					.collect(Collectors.toList());

			for (DistanceDistribution.DistanceBin bin : bins) {

				sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(bin.getValue() * scalingFactor);
				rowIndex++;
			}
			rowIndex = 1;
			cellIndex++;
		}


		try (OutputStream fileOut = new FileOutputStream("C:\\Users\\Janek\\Desktop\\test.xlsx")) {
			wb.write(fileOut);
		}
	}

	private int compareBinsByModeAndDistanceRange(DistanceDistribution.DistanceBin bin1, DistanceDistribution.DistanceBin bin2) {
		int mode = bin1.getMode().compareTo(bin2.getMode());
		return (mode == 0) ? Double.compare(bin1.getDistanceRange().getLowerLimit(), bin2.getDistanceRange().getLowerLimit()) : mode;
	}

	private String convertDistanceDistributionToString(DistanceDistribution input) {
		return input.getDistanceBins().stream()
				.sorted((bin1, bin2) -> {
					int mode = bin1.getMode().compareTo(bin2.getMode());
					return (mode == 0) ? Double.compare(bin1.getDistanceRange().getLowerLimit(), bin2.getDistanceRange().getLowerLimit()) : mode;
				})
				.map(bin -> " " + bin.getMode() + " " + bin.getValue())
				.reduce(String::concat)
				.orElseThrow(() -> new RuntimeException("error0"));
	}

	private DefaultCategoryDataset createDataset(Collection<NamedDistanceDistribution> distanceDistributions) {

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (NamedDistanceDistribution ndd : distanceDistributions) {

			DistanceDistribution distribution = ndd.distanceDistribution;

			for (DistanceDistribution.DistanceBin distanceBin : ndd.distanceDistribution.getDistanceBins()) {
				dataset.addValue(distanceBin.getValue(), ndd.name, distanceBin.getMode());
			}
		}
		return dataset;

	}

	private NamedDistanceDistribution parseEventFile(Path file, DistanceDistribution expectedDistribution) {

		EventsManager manager = EventsUtils.createEventsManager();
		TripEventHandler tripEventHandler = new TripEventHandler();
		manager.addHandler(tripEventHandler);
		new MatsimEventsReader(manager).readFile(file.toString());

		DistanceDistribution simulatedDistribution = expectedDistribution.copyWithEmptyBins();

		tripEventHandler.personTrips.entrySet().parallelStream().flatMap(entry -> entry.getValue().stream())
				.forEach(trip -> {
					double distance = calculateBeelineDistance(trip);
					simulatedDistribution.increaseCountByOne(trip.mainMode, distance);
				});

		String runId = file.getFileName().toString().split("[.]")[0];
		return new NamedDistanceDistribution(runId, simulatedDistribution);
	}

	private double calculateBeelineDistance(TripEventHandler.Trip trip) {

		if (scenario.getActivityFacilities().getFacilities().containsKey(trip.departureFacility) ||
				scenario.getActivityFacilities().getFacilities().containsKey(trip.arrivalFacility)
		) {
			ActivityFacility departureFacility = scenario.getActivityFacilities().getFacilities().get(trip.departureFacility);
			ActivityFacility arrivalFacility = scenario.getActivityFacilities().getFacilities().get(trip.arrivalFacility);
			return CoordUtils.calcEuclideanDistance(departureFacility.getCoord(), arrivalFacility.getCoord());
		} else {
			Link departureLink = network.getLinks().get(trip.departureLink);
			Link arrivalLink = network.getLinks().get(trip.arrivalLink);
			return CoordUtils.calcEuclideanDistance(departureLink.getToNode().getCoord(), arrivalLink.getToNode().getCoord());
		}
	}

	private static class TripEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, TransitDriverStartsEventHandler {

		private final Predicate<Id<Person>> agentFilter;
		private final Set<Id<Person>> transitDrivers = new HashSet<>();
		private final Map<Id<Person>, List<Trip>> personTrips = new HashMap<>();
		private final Set<Id<Person>> stuckPersons = new HashSet<>();
		private final MainModeIdentifier mainModeIdentifier = new NemoModeLocationChoiceMainModeIdentifier();

		TripEventHandler(Predicate<Id<Person>> agentFilter) {
			this.agentFilter = agentFilter;
		}

		TripEventHandler() {
			this(id -> true);
		}

		@Override
		public void handleEvent(ActivityEndEvent activityEndEvent) {

			if (StageActivityTypeIdentifier.isStageActivity(activityEndEvent.getActType())
					|| transitDrivers.contains(activityEndEvent.getPersonId())
					|| !agentFilter.test(activityEndEvent.getPersonId()))
				return;

			if (!personTrips.containsKey(activityEndEvent.getPersonId())) {
				List<Trip> trips = new ArrayList<>();
				personTrips.put(activityEndEvent.getPersonId(), trips);
			}

			Trip trip = new Trip();
			trip.departureTime = activityEndEvent.getTime();
			trip.departureLink = activityEndEvent.getLinkId();
			trip.departureFacility = activityEndEvent.getFacilityId();

			personTrips.get(activityEndEvent.getPersonId()).add(trip);
		}

		@Override
		public void handleEvent(ActivityStartEvent activityStartEvent) {

			if (StageActivityTypeIdentifier.isStageActivity(activityStartEvent.getActType()) || !personTrips.containsKey(activityStartEvent.getPersonId()))
				return;

			Trip trip = getCurrentTrip(activityStartEvent.getPersonId());
			trip.arrivalTime = activityStartEvent.getTime();
			trip.arrivalLink = activityStartEvent.getLinkId();
			trip.arrivalFacility = activityStartEvent.getFacilityId();

			try {
				trip.mainMode = mainModeIdentifier.identifyMainMode(trip.legs);
			} catch (Exception e) {
				// the default main mode identifier can't handle non-network-walk only
				trip.mainMode = TransportMode.non_network_walk;
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent personArrivalEvent) {

			if (!personTrips.containsKey(personArrivalEvent.getPersonId()))
				return;

			Trip trip = getCurrentTrip(personArrivalEvent.getPersonId());
			Leg leg = trip.legs.get(trip.legs.size() - 1);
			leg.setTravelTime(personArrivalEvent.getTime() - leg.getDepartureTime());
		}

		@Override
		public void handleEvent(PersonDepartureEvent personDepartureEvent) {
			if (!personTrips.containsKey(personDepartureEvent.getPersonId()))
				return;

			Leg leg = PopulationUtils.createLeg(personDepartureEvent.getLegMode());
			leg.setDepartureTime(personDepartureEvent.getTime());
			leg.setMode(personDepartureEvent.getLegMode());
			Trip trip = getCurrentTrip(personDepartureEvent.getPersonId());
			trip.legs.add(leg);
		}

		@Override
		public void handleEvent(PersonStuckEvent personStuckEvent) {

			personTrips.remove(personStuckEvent.getPersonId());
			stuckPersons.add(personStuckEvent.getPersonId());
		}

		@Override
		public void handleEvent(TransitDriverStartsEvent transitDriverStartsEvent) {
			transitDrivers.add(transitDriverStartsEvent.getDriverId());
		}

		private Trip getCurrentTrip(Id<Person> personId) {

			List<Trip> trips = personTrips.get(personId);
			return trips.get(trips.size() - 1);
		}

		private static class Trip {

			private Id<Link> departureLink;
			private Id<Link> arrivalLink;
			private double departureTime;
			private double arrivalTime;
			private Id<ActivityFacility> departureFacility;
			private Id<ActivityFacility> arrivalFacility;
			private String mainMode = TransportMode.other;

			private List<Leg> legs = new ArrayList<>();

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
