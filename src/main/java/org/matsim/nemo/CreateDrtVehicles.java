package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateDrtVehicles {

	/**
	 * Adjust these variables and paths to your need.
	 */

	private static final int numberOfVehicles = 1500;
	private static final int seatsPerVehicle = 6; //this is important for DRT, value is not used by taxi
	private static final double operationStartTime = 0;
	private static final double operationEndTime = 24 * 60 * 60; //24h
	private static final Random random = MatsimRandom.getRandom();

	private static final String networkPath = "projects\\nemo_mercator\\data\\matsim_input\\supply\\bikeHighways\\nemo_bikeHighways_network.xml.gz";
	private static final String shapeFilePath = "projects\\nemo_mercator\\data\\original_files\\shapeFiles\\shapeFile_Ruhrgebiet\\ruhrgebiet_boundary.shp";
	private static final String outputPath = "projects\\nemo_mercator\\data\\matsim_input\\supply\\smartCity\\drt_vehicles.xml.gz";

	private final Network network;
	private final Collection<Geometry> serviceArea;
	private final Path output;

    private CreateDrtVehicles(Network network, Collection<Geometry> geometries, Path output) {
		this.network = network;
		this.serviceArea = geometries;
		this.output = output;
	}

    public static void main(String[] args) throws IOException {

		InputArguments arguments = new InputArguments();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

        //create output directory if necessary
        Files.createDirectories(Paths.get(arguments.svnDir).resolve(outputPath).getParent());

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(
				Paths.get(arguments.svnDir).resolve(shapeFilePath).toString());
		Collection<Geometry> geometries = features.stream()
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.collect(Collectors.toList());
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(Paths.get(arguments.svnDir).resolve(networkPath).toString());
		new CreateDrtVehicles(network, geometries, Paths.get(arguments.svnDir).resolve(outputPath)).run();
	}

	private void run() {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Stream<DvrpVehicleSpecification> vehicleSpecificationStream = network.getLinks().entrySet().parallelStream()
                .filter(entry -> entry.getValue().getAllowedModes().contains(TransportMode.car))// drt can only start on links with Transport mode 'car'
				.filter(entry -> isInServiceArea(entry.getValue()))
				.sorted((e1, e2) -> (random.nextInt(2) - 1)) // shuffle links
				.limit(numberOfVehicles) // select the first *numberOfVehicles* links
				.map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
                        .id(Id.create("drt_" + random.nextInt(), DvrpVehicle.class))
						.startLinkId(entry.getKey())
						.capacity(seatsPerVehicle)
						.serviceBeginTime(operationStartTime)
						.serviceEndTime(operationEndTime)
						.build());

        new ConcurrentFleetWriter(vehicleSpecificationStream).write(output.toString());
	}

	private boolean isInServiceArea(Link link) {

		Point centerOfLink = MGC.coord2Point(link.getCoord());
		return serviceArea.stream().anyMatch(geometry -> geometry.contains(centerOfLink));

	}

	private static class InputArguments {

		@Parameter(names = "-svnDir", required = true)
		private String svnDir;
	}
}

/**
 * Same as FleetWriter from dvrp package but allows parallel Streams
 */
class ConcurrentFleetWriter extends MatsimXmlWriter {

    private Stream<? extends DvrpVehicleSpecification> vehicleSpecifications;

    ConcurrentFleetWriter(Stream<? extends DvrpVehicleSpecification> vehicleSpecifications) {
        this.vehicleSpecifications = vehicleSpecifications;
    }

    public void write(String file) {
        this.openFile(file);
        this.writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
        this.writeStartTag("vehicles", Collections.emptyList());
        this.vehicleSpecifications.forEach(this::writeVehicle);
        this.writeEndTag("vehicles");
        this.close();
    }

    private synchronized void writeVehicle(DvrpVehicleSpecification vehicle) {
        List<Tuple<String, String>> atts = Arrays.asList(Tuple.of("id", vehicle.getId().toString()), Tuple.of("start_link", vehicle.getStartLinkId() + ""), Tuple.of("t_0", vehicle.getServiceBeginTime() + ""), Tuple.of("t_1", vehicle.getServiceEndTime() + ""), Tuple.of("capacity", vehicle.getCapacity() + ""));
        this.writeStartTag("vehicle", atts, true);
    }
}
