package org.matsim.nemo;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

public class BanCarsFromLivingStreets {

	private static final String inputNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.0-1pct/input/ruhrgebiet-v1.0-network-with-RSV.xml.gz";
	private static final String ruhrShape = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/original_data/shapes/ruhrgebiet_boundary.shp";
	private static final String outputNetwork = "C:\\Users\\Janek\\repos\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\healthy\\network.xml.gz";

	public static void main(String[] args) throws MalformedURLException {

		var network = NetworkUtils.readNetwork(inputNetwork);
		var shape = ShapeFileReader.getAllFeatures(new URL(ruhrShape)).stream()
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.collect(Collectors.toList());

		System.out.println("excluding cars on residential streets");
		// ban cars from residential streets
		network.getLinks().values().parallelStream()
				.filter(link -> link.getAttributes().getAttribute("type") != null)
				.filter(link -> link.getAttributes().getAttribute("type").equals("living_street") || link.getAttributes().getAttribute("type").equals("residential"))
				.filter(link -> shape.stream().anyMatch(g -> g.contains(MGC.coord2Point(link.getCoord()))))
				.forEach(link -> {
					// remove car modes
					var allowedModes = link.getAllowedModes().stream()
							.filter(mode -> !mode.equals(TransportMode.car) && !mode.equals(TransportMode.ride))
							.collect(Collectors.toSet());
					link.setAllowedModes(allowedModes);
				});

		System.out.println("creating extra bike links");
		// give it some bike capacity
		var bikeLinks = network.getLinks().values().parallelStream()
				.filter(link -> link.getCapacity() > 1000)
				.filter(link -> link.getAllowedModes().contains(TransportMode.bike))
				.filter(link -> link.getAllowedModes().contains(TransportMode.car))
				.map(link -> {
					var bikeLinkId = Id.createLinkId(link.getId() + "_bike");
					var bikeLink = network.getFactory().createLink(bikeLinkId, link.getFromNode(), link.getToNode());
					bikeLink.setAllowedModes(Set.of(TransportMode.bike));
					bikeLink.setLength(link.getLength());
					bikeLink.setFreespeed(link.getFreespeed());
					bikeLink.setNumberOfLanes(1);

					var attrs = link.getAttributes().getAsMap();
					for (var entry : attrs.entrySet()) {
						bikeLink.getAttributes().putAttribute(entry.getKey(), entry.getValue());
					}

					if (link.getCapacity() < 2000) {
						bikeLink.setCapacity(link.getCapacity() - 1000);
						link.setCapacity(1000);
					} else {
						bikeLink.setCapacity(1000);
						link.setCapacity(link.getCapacity() - 1000);
					}

					var allowedModes = link.getAllowedModes().stream()
							.filter(mode -> !mode.equals(TransportMode.bike))
							.collect(Collectors.toSet());
					link.setAllowedModes(allowedModes);

					return bikeLink;
				})
				.collect(Collectors.toList());

		System.out.println("Adding links to network");
		for (Link link : bikeLinks) {
			network.addLink(link);
		}


		new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
		new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.ride));
		new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));
		new NetworkWriter(network).write(outputNetwork);
	}
}
