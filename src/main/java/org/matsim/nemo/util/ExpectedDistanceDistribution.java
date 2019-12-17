package org.matsim.nemo.util;

import org.matsim.nemo.analysis.SimpleDistanceDistribution;

public class ExpectedDistanceDistribution {

	public static SimpleDistanceDistribution create() {
		SimpleDistanceDistribution distanceDistribution = new SimpleDistanceDistribution();
		distanceDistribution.add(0, 1000, 1937700);
		distanceDistribution.add(1000, 3000, 3684000);
		distanceDistribution.add(3000, 5000, 2272800);
		distanceDistribution.add(5000, 10000, 2706200);
		distanceDistribution.add(10000, 1000000, 3302700);
		return distanceDistribution;
	}
}
