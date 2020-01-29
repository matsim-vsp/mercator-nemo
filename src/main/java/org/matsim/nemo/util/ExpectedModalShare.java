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

	public static void writeToCsv(Path file) throws IOException {

		try (var writer = Files.newBufferedWriter(file)) {
			try (var printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
				printer.printRecord("mode", "value");
				for (Map.Entry<String, Long> mode : create().entrySet()) {
					printer.printRecord(mode.getKey(), mode.getValue());
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {

		if (args.length != 1) throw new RuntimeException("Give an output file path!");

		writeToCsv(Paths.get(args[0]));
	}
}
