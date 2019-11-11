package org.matsim.nemo.util;

import org.matsim.api.core.v01.TransportMode;
import playground.vsp.cadyts.marginals.DistanceDistribution;

public class ExpectedModalDistanceDistribution {

	public static DistanceDistribution create() {
		DistanceDistribution distanceDistribution = new DistanceDistribution();
		distanceDistribution.add(TransportMode.car, 0, 1000, 10000, 357488);
		distanceDistribution.add(TransportMode.pt, 0, 1000, 10000, 28540);
		distanceDistribution.add(TransportMode.bike, 0.0, 1000, 10000, 212908);
		distanceDistribution.add(TransportMode.walk, 0.0, 1000, 10000, 1842358);
		distanceDistribution.add(TransportMode.ride, 0.0, 1000, 1000, 124379);

		distanceDistribution.add(TransportMode.car, 1000.0, 3000, 10000, 1213004);
		distanceDistribution.add(TransportMode.pt, 1000.0, 3000, 10000, 437762);
		distanceDistribution.add(TransportMode.bike, 1000.0, 3000, 10000, 541548);
		distanceDistribution.add(TransportMode.walk, 1000.0, 3000, 10000, 832557);
		distanceDistribution.add(TransportMode.ride, 1000.0, 3000, 1000, 483180);

		distanceDistribution.add(TransportMode.car, 3000.0, 5000, 10000, 842537);
		distanceDistribution.add(TransportMode.pt, 3000.0, 5000, 10000, 323085);
		distanceDistribution.add(TransportMode.bike, 3000.0, 5000, 10000, 328707);
		distanceDistribution.add(TransportMode.walk, 3000.0, 5000, 10000, 132547);
		distanceDistribution.add(TransportMode.ride, 3000.0, 5000, 1000, 232588);

		distanceDistribution.add(TransportMode.car, 5000.0, 10000, 10000, 1274829);
		distanceDistribution.add(TransportMode.pt, 5000.0, 10000, 10000, 326567);
		distanceDistribution.add(TransportMode.bike, 5000.0, 10000, 10000, 99804);
		distanceDistribution.add(TransportMode.walk, 5000.0, 10000, 10000, 68559);
		distanceDistribution.add(TransportMode.ride, 5000.0, 10000, 1000, 325927);

		distanceDistribution.add(TransportMode.car, 10000.0, 1000000, 10000, 1979366);
		distanceDistribution.add(TransportMode.pt, 10000.0, 1000000, 10000, 456848);
		distanceDistribution.add(TransportMode.bike, 10000.0, 1000000, 10000, 63259);
		distanceDistribution.add(TransportMode.walk, 10000.0, 1000000, 10000, 33138);
		distanceDistribution.add(TransportMode.ride, 10000.0, 1000000, 1000, 322899);

		return distanceDistribution;
	}
}
