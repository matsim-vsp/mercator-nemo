package org.matsim.nemo.util;

import org.matsim.nemo.analysis.SimpleDistanceDistribution;

public class ExpectedDistanceDistribution {

	public static SimpleDistanceDistribution create() {
		SimpleDistanceDistribution distanceDistribution = new SimpleDistanceDistribution();
		distanceDistribution.add(0, 1000, 2565675);
		distanceDistribution.add(1000, 3000, 3508053);
		distanceDistribution.add(3000, 5000, 1859467);
		distanceDistribution.add(5000, 10000, 2095688);
		distanceDistribution.add(10000, 1000000, 2855513);
		return distanceDistribution;
	}
}
