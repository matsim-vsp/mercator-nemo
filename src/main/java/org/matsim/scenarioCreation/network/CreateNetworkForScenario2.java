package org.matsim.scenarioCreation.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.scenarioCreation.pt.CreateScenarioFromOsmFile;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleWriterV1;

import com.vividsolutions.jts.geom.Geometry;

public class CreateNetworkForScenario2 {

	public static void main(String[] args) {

		// später hier oben alles wie bei anderen create network Klassen

		// erstmal testweise bestehendes network und shape einlesen
//		Network network = NetworkUtils.readNetwork(
//				"C:\\Users\\Karoline\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\network\\fine\\nemo_fine_network.xml");
//		String shpFile = "C:\\Users\\Karoline\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\shapeFiles\\shapeFile_Ruhrgebiet\\ruhrgebiet_boundary.shp";
		// Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// new TransitScheduleReader(scenario).readFile(
		// "C:\\Users\\Karoline\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\pt\\transit_schedule.xml.gz");
		// TransitSchedule schedule = scenario.getTransitSchedule();

//		List<Geometry> geometries = new ArrayList<>();
//		ShapeFileReader.getAllFeatures(shpFile)
//				.forEach(feature -> geometries.add((Geometry) feature.getDefaultGeometry()));
//
//		banCarfromResidentialAreasAndCreateBikeLinks(network, geometries);

//		new NetworkWriter(network).write("C:\\Users\\Karoline\\nemo-data\\network_carsBannedBikeCapa.xml");

		String osmFile = "C:\\Users\\Karoline\\shared-svn\\projects\\nemo_mercator\\data\\pt\\ptNetworkScheduleFileFromOSM.xml";
		String osmScheduleOutFile = "C:\\Users\\Karoline\\nemo-data\\osmScheduleAfterChanges.xml";
		String osmVehicleOutFile = "C:\\Users\\Karoline\\nemo-data\\osmVehiclesAfterChanges.xml";

		Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile().run(osmFile);

		addMorePtDepartures(scenarioFromOsmSchedule);
		new VehicleWriterV1(scenarioFromOsmSchedule.getTransitVehicles()).writeFile(osmVehicleOutFile);
		new TransitScheduleWriterV2(scenarioFromOsmSchedule.getTransitSchedule()).write(osmScheduleOutFile);

	}

	public static void banCarfromResidentialAreasAndCreateBikeLinks(Network network, List<Geometry> geometries) {

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

				// if capacity < 1000. no extra bike link

				if (capacity > 1000.) {
					// new bike link
					Id<Link> bikeLinkId = Id.createLinkId(link.getId() + "_bike");
					Link bikeLink = factory.createLink(bikeLinkId, link.getFromNode(), link.getToNode());
					Set<String> allowedModes = new HashSet<>();
					allowedModes.add(TransportMode.bike);
					bikeLink.setAllowedModes(allowedModes);
					bikeLink.setLength(link.getLength());
					bikeLink.setFreespeed(link.getFreespeed());
					bikeLink.getAttributes().putAttribute("type", link.getAttributes().getAttribute("type"));

					// lanes? oneway? attributes origid+surface?

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

	public static boolean linkIsInShape(Link link, List<Geometry> geometries) {

		Coord fromCoord = link.getFromNode().getCoord();
		Coord toCoord = link.getToNode().getCoord();
		boolean fromCoordInShape = geometries.stream()
				.anyMatch(geometry -> geometry.contains(MGC.coord2Point(fromCoord)));
		boolean toCoordInShape = geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(toCoord)));

		if (fromCoordInShape && toCoordInShape) {
			return true;
		} else {
			return false;
		}

	}

	public static void addMorePtDepartures(Scenario scenario) {

		TransitSchedule schedule = scenario.getTransitSchedule();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {

				// putting all departure times in a list to sort them
				List<Departure> departures = new ArrayList<>();

				for (Departure departure : route.getDepartures().values()) {
					departures.add(departure);
				}

				// sort departures by departure time
				departures.sort(Comparator.comparing(Departure::getDepartureTime));

				for (Departure dep : departures) {

					int index = departures.indexOf(dep);
					// add departures in between

					if (index != 0) {
						// calculate intervall (not possible for first dep)
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

	public static Departure createVehicleAndReturnDeparture(Scenario scenario, Departure oldDeparture, double t) {

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
