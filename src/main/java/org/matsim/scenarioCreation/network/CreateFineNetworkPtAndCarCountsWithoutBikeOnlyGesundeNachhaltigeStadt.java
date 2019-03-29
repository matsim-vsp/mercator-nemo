package org.matsim.scenarioCreation.network;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.scenarioCreation.counts.CombinedCountsWriter;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;
import org.matsim.scenarioCreation.pt.CreateScenarioFromGtfs;
import org.matsim.scenarioCreation.pt.CreateScenarioFromOsmFile;
import org.matsim.scenarioCreation.pt.PtInput;
import org.matsim.scenarioCreation.pt.PtOutput;
import org.matsim.util.NEMOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class CreateFineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt {

	private static final String SUBDIR = "fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt";
	private static final String FILE_PREFIX = "fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt";
	private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt.class);

	public static void main(String[] args) throws IOException {

		// parse input
		InputArguments arguments = new InputArguments();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		NetworkOutput outputParams = new NetworkOutput(arguments.svnDir);
		NetworkInput inputParams = new NetworkInput(arguments.svnDir);
		PtInput ptInputParams = new PtInput(arguments.svnDir);
		PtOutput ptOutputParams = new PtOutput(arguments.svnDir);

		// ensure all output directories are present
		Files.createDirectories(outputParams.getOutputNetworkDir().resolve(SUBDIR));

		// read in transit-network, vehicles and schedule from osm and double the number of departures
		Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile()
				.run(ptInputParams.getOsmScheduleFile().toString());
		addMorePtDepartures(scenarioFromOsmSchedule);

		// read in transit-network, vehicles and schedule from gtfs
		Scenario scenarioFromGtfsSchedule = new CreateScenarioFromGtfs().run(ptInputParams.getGtfsFile().toString());

		// merge two transit networks into gtfs network
		MergeNetworks.merge(scenarioFromGtfsSchedule.getNetwork(), "", scenarioFromOsmSchedule.getNetwork());
		mergeSchedules(scenarioFromGtfsSchedule.getTransitSchedule(), scenarioFromOsmSchedule.getTransitSchedule());
		mergeVehicles(scenarioFromGtfsSchedule.getTransitVehicles(), scenarioFromOsmSchedule.getTransitVehicles());

		new VehicleWriterV1(scenarioFromGtfsSchedule.getTransitVehicles())
				.writeFile(ptOutputParams.getTransitVehiclesFile().toString());
		new TransitScheduleWriterV2(scenarioFromGtfsSchedule.getTransitSchedule())
				.write(ptOutputParams.getTransitScheduleFile().toString());

		// create the network from scratch
		NetworkCreator creator = new NetworkCreator.Builder().setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
				.setSvnDir(arguments.svnDir).withByciclePaths()
				.withOsmFilter(new FineNetworkFilterWithoutBikeOnlyLinks(inputParams.getInputNetworkShapeFilter()))
				.withCleaningModes(TransportMode.car, TransportMode.ride, TransportMode.bike).withRideOnCarLinks()
				.build();
		Network network = creator.createNetwork();
		
		banCarfromResidentialAreasAndCreateBikeLinks(network, inputParams.getInputNetworkShapeFilter());

		logger.info("merge transit networks and car/ride/bike network");
		MergeNetworks.merge(network, "", scenarioFromGtfsSchedule.getNetwork());

		final Path outputNetwork = outputParams.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + ".xml.gz");
		logger.info("writing network to: " + outputNetwork.toString());
		new NetworkWriter(network).write(outputNetwork.toString());

		// create long term counts
		Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
		NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
				.setSvnDir(arguments.svnDir).withNetwork(network).withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 50025L).build();
		Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

		// create short term counts
		NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
				.setSvnDir(arguments.svnDir).withNetwork(network).withColumnCombinations(columnCombinations)
				.withStationIdsToOmit(5002L, 5025L).build();
		Map<String, Counts<Link>> shortTermCounts = shortTermCountsCreator.run();

		CombinedCountsWriter.writeCounts(outputParams.getOutputNetworkDir().resolve(SUBDIR), FILE_PREFIX,
				columnCombinations, longTermCounts, shortTermCounts);

	}

	private static void mergeSchedules(TransitSchedule schedule, TransitSchedule toBeMerged) {
		toBeMerged.getFacilities().values().forEach(schedule::addStopFacility);
		toBeMerged.getTransitLines().values().forEach(schedule::addTransitLine);
	}

	private static void mergeVehicles(Vehicles vehicles, Vehicles toBeMerged) {
		toBeMerged.getVehicleTypes().values().forEach(vehicles::addVehicleType);
		toBeMerged.getVehicles().values().forEach(vehicles::addVehicle);
	}

	private static class InputArguments {

		@Parameter(names = "-svnDir", required = true, description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
		private String svnDir;
	}

	private static void banCarfromResidentialAreasAndCreateBikeLinks(Network network, String shpFile) {

		List<Geometry> geometries = new ArrayList<>();
		ShapeFileReader.getAllFeatures(shpFile)
				.forEach(feature -> geometries.add((Geometry) feature.getDefaultGeometry()));

		NetworkFactory factory = network.getFactory();
		List<Link> newBikeLinks = new ArrayList<>();

		for (Link link : network.getLinks().values()) {
			if (link.getAttributes().getAttribute("type").equals("residential") && linkIsInShape(link, geometries)) {
				// link is residential and in shapeFile --> ban cars
				Set<String> oldAllowedModes = link.getAllowedModes();
				Set<String> newAllowedModes = new HashSet<>();
				for (String mode : oldAllowedModes) {
					if (!mode.equals("car") && !mode.equals("ride")) {
						newAllowedModes.add(mode);
					}
				}
				link.setAllowedModes(newAllowedModes);

			} else { // all non-residential links and residential outside of shape --> bike capacity

				double capacity = link.getCapacity();

				// if capacity > 1000. extra bike link, if lower no bike link

				if (capacity > 1000.) {
					// new bike link
					Id<Link> bikeLinkId = Id.createLinkId(link.getId() + "_bike");
					Link bikeLink = factory.createLink(bikeLinkId, link.getFromNode(), link.getToNode());
					Set<String> allowedModes = new HashSet<>();
					allowedModes.add(TransportMode.bike);
					bikeLink.setAllowedModes(allowedModes);
					bikeLink.setLength(link.getLength());
					bikeLink.setFreespeed(link.getFreespeed());
					bikeLink.setNumberOfLanes(1);

					Map<String, Object> attributes = link.getAttributes().getAsMap();
					for (Entry<String, Object> entry : attributes.entrySet()) {
						bikeLink.getAttributes().putAttribute(entry.getKey(), entry.getValue());
					}

					// set capacity in new bike link and old link according to Copenhagen model
					if (capacity < 2000) {
						// 1000<cap<2000
						bikeLink.setCapacity(capacity - 1000.);
						link.setCapacity(1000);

					} else {
						// 2000<cap
						bikeLink.setCapacity(1000);
						link.setCapacity(capacity - 1000.);
					}

					newBikeLinks.add(bikeLink);

					// remove bike from old link
					Set<String> oldAllowedModes = link.getAllowedModes();
					Set<String> newAllowedModes = new HashSet<>();
					for (String mode : oldAllowedModes) {
						if (!mode.equals(TransportMode.bike)) {
							newAllowedModes.add(mode);
						}
					}
					link.setAllowedModes(newAllowedModes);
				}
			}
		}
		for (Link bikeLink : newBikeLinks) {
			network.addLink(bikeLink);
		}
	}

	private static boolean linkIsInShape(Link link, List<Geometry> geometries) {

		Coord fromCoord = link.getFromNode().getCoord();
		Coord toCoord = link.getToNode().getCoord();
		boolean fromCoordInShape = geometries.stream()
				.anyMatch(geometry -> geometry.contains(MGC.coord2Point(fromCoord)));
		boolean toCoordInShape = geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(toCoord)));

		return fromCoordInShape && toCoordInShape;

	}

	private static void addMorePtDepartures(Scenario scenario) {

		TransitSchedule schedule = scenario.getTransitSchedule();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {

				// putting all departures in a list to sort them by time

				List<Departure> departures = new ArrayList<>(route.getDepartures().values());

				departures.sort(Comparator.comparing(Departure::getDepartureTime));

				for (Departure dep : departures) {

					int index = departures.indexOf(dep);
					// add departures in between

					if (index != 0) {
						// calculate interval (not possible for first departure)
						double previousDepTime = departures.get(index - 1).getDepartureTime();
						double depTime = dep.getDepartureTime();
						double interval = depTime - previousDepTime;
						double newInterval = interval / 2.;

						if (index == 1) {
							// second departure: add new departure before and after
							Departure newDepBefore = createVehicleAndReturnDeparture(scenario,
									departures.get(index - 1), dep.getDepartureTime() - newInterval);
							Departure newDepAfter = createVehicleAndReturnDeparture(scenario, dep,
									dep.getDepartureTime() + newInterval);
							route.addDeparture(newDepAfter);
							route.addDeparture(newDepBefore);

						} else {
							// all other: add new departure after
							Departure newDepAfter = createVehicleAndReturnDeparture(scenario, dep,
									dep.getDepartureTime() + newInterval);
							route.addDeparture(newDepAfter);
						}
					}
				}
			}
		}
	}

	private static Departure createVehicleAndReturnDeparture(Scenario scenario, Departure oldDeparture, double t) {

		// create vehicle
		Id<Vehicle> oldVehicleId = oldDeparture.getVehicleId();
		Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(
				Id.createVehicleId(oldVehicleId + "_2"),
				scenario.getTransitVehicles().getVehicles().get(oldVehicleId).getType());
		scenario.getTransitVehicles().addVehicle(vehicle);

		// create departure
		Departure departure = scenario.getTransitSchedule().getFactory()
				.createDeparture(Id.create(oldDeparture.getId() + "_2", Departure.class), t);
		departure.setVehicleId(vehicle.getId());
		return departure;
	}

}
