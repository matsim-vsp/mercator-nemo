package org.matsim.nemo.util;

import org.matsim.api.core.v01.TransportMode;

import java.util.HashMap;
import java.util.Map;

public class ExpectedModalShare {

	public static Map<String, Long> create() {
		Map<String, Long> result = new HashMap<>();
		result.put(TransportMode.car, 5667224L);
		result.put(TransportMode.bike, 1246226L);
		result.put(TransportMode.pt, 1572802L);
		result.put(TransportMode.ride, 1488973L);
		result.put(TransportMode.walk, 2909159L);
		return result;
	}
}
