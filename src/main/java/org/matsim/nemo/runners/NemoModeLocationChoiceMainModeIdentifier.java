package org.matsim.nemo.runners;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import java.util.List;

/**
 * This main mode identifier identifies main modes according to the MiD (Mobilitaet in Deutschland). Main modes are chosen
 * in the following order pt, drt, car, ride, bike, walk.
 * <p>
 * Example: If a Trip consists of Three legs with modes: walk, pt, drt; the main mode of the trip is going to be pt, since
 * it is the highest ranking mode.
 */
public class NemoModeLocationChoiceMainModeIdentifier implements MainModeIdentifier {

	private final List<String> modes = List.of(TransportMode.walk, TransportMode.bike, TransportMode.ride, TransportMode.car, TransportMode.drt, TransportMode.pt);

	@Override
	public String identifyMainMode(List<? extends PlanElement> list) {

		return list.stream()
				.filter(element -> element instanceof Leg)
				.map(element -> (Leg) element)
				// all the weird walking stuff is considered walk in our case
				.map(leg -> leg.getMode().equals(TransportMode.non_network_walk) || leg.getMode().equals(TransportMode.transit_walk) ? TransportMode.walk : leg.getMode())
				.min(this::compareModes)
				.orElse(TransportMode.other);
	}

	private int compareModes(String mode1, String mode2) {

		int index1 = modes.indexOf(mode1);
		int index2 = modes.indexOf(mode2);

		return index2 - index1;
	}
}
