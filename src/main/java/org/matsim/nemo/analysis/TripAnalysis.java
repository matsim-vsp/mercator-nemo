package org.matsim.nemo.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TripAnalysis {

	private final Scenario scenario;
	private final Network network;
	private final Map<Id<Person>, List<TripEventHandler.Trip>> personTrips;

	public TripAnalysis(Map<Id<Person>, List<TripEventHandler.Trip>> personTrips, Scenario scenario, Network network) {
		this.scenario = scenario;
		this.network = network;
		this.personTrips = personTrips;
	}

	public DistanceDistribution calculateModalDistanceDistribution(DistanceDistribution expectedDistribution) {

		DistanceDistribution simulatedDistribution = expectedDistribution.copyWithEmptyBins();
		personTrips.entrySet().parallelStream().flatMap(entry -> entry.getValue().stream())
				.forEach(trip -> {
					double distance = calculateBeelineDistance(trip);
					simulatedDistribution.increaseCountByOne(trip.getMainMode(), distance);
				});
		return simulatedDistribution;
	}

	public SimpleDistanceDistribution calculateDistanceDistribution(SimpleDistanceDistribution expectedDistribution) {
		SimpleDistanceDistribution simulatedDistribution = expectedDistribution.copyWithEmptyBins();
		personTrips.entrySet().parallelStream().
				flatMap(entry -> entry.getValue().stream())
				.forEach(trip -> {
					double distance = calculateBeelineDistance(trip);
					simulatedDistribution.increaseCountByOne(distance);
				});
		return simulatedDistribution;
	}

	public Map<String, Long> calculateModalSplit() {

		return personTrips.values().parallelStream()
				.flatMap(Collection::stream)
				.collect(Collectors.groupingBy(TripEventHandler.Trip::getMainMode, Collectors.counting()));
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
