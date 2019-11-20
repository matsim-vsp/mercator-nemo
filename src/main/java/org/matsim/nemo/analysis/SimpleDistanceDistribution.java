package org.matsim.nemo.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimpleDistanceDistribution {

	private final List<SimpleDistanceBin> bins = new ArrayList<>();

	public void add(double lowerLimit, double upperLimit, int value) {
		bins.add(new SimpleDistanceBin(lowerLimit, upperLimit, value));
	}

	public void increaseCountByOne(double distance) {

		SimpleDistanceBin binForDistance = bins.stream()
				.filter(bin -> bin.distanceRange.isWithinRange(distance))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("no bin for distance of: " + distance + " was found"));

		binForDistance.increaseCountByOne();
	}

	public List<SimpleDistanceBin> getDistanceBins() {
		return new ArrayList<>(bins);
	}

	public SimpleDistanceDistribution copyWithEmptyBins() {
		SimpleDistanceDistribution result = new SimpleDistanceDistribution();
		for (SimpleDistanceBin bin : bins) {
			result.add(bin.distanceRange.lowerLimit, bin.distanceRange.upperLimit, 0);
		}
		return result;
	}

	public static class SimpleDistanceBin {

		private final SimpleDistanceRange distanceRange;
		private int value;

		public SimpleDistanceBin(double lowerLimit, double upperLimit, int value) {
			this.distanceRange = new SimpleDistanceRange(lowerLimit, upperLimit);
			this.value = value;
		}

		void increaseCountByOne() {
			this.value++;
		}
	}

	public static class SimpleDistanceRange {

		private static final Comparator<SimpleDistanceRange> comparator = Comparator.comparingDouble(
				other -> other.lowerLimit
		);
		private final double lowerLimit;
		private final double upperLimit;

		SimpleDistanceRange(double lowerLimit, double upperLimit) {
			this.lowerLimit = lowerLimit;
			this.upperLimit = upperLimit;
		}

		public double getLowerLimit() {
			return lowerLimit;
		}

		public double getUpperLimit() {
			return upperLimit;
		}

		boolean isWithinRange(double distance) {
			return lowerLimit <= distance && distance <= upperLimit;
		}
	}
}
