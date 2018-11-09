package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.scenarioCreation.counts.CombinedCountsWriter;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;
import org.matsim.scenarioCreation.pt.CreateScenarioFromGtfs;
import org.matsim.scenarioCreation.pt.CreateScenarioFromOsmFile;
import org.matsim.scenarioCreation.pt.PtInput;
import org.matsim.scenarioCreation.pt.PtOutput;
import org.matsim.util.NEMOUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CreateFineNetworkWithPtAndCarCounts {

    private static final String SUBDIR = "fine_with-pt";
    private static final String FILE_PREFIX = "nemo_fine_network_with-pt";
    private static final Logger logger = LoggerFactory.getLogger(CreateFineNetworkWithPtAndCarCounts.class);

    public static void main(String[] args) throws IOException {

        // parse input variables
        InputArguments arguments = new CreateFineNetworkWithPtAndCarCounts.InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        NetworkOutput networkOutputParams = new NetworkOutput(arguments.svnDir);
        NetworkInput networkInputParams = new NetworkInput(arguments.svnDir);
        PtInput ptInputParams = new PtInput(arguments.svnDir);
        PtOutput ptOutputParams = new PtOutput(arguments.svnDir);

        // ensure output folder is present
        final Path outputNetwork = networkOutputParams.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + ".xml.gz");
        Files.createDirectories(outputNetwork.getParent());
        Files.createDirectories(ptOutputParams.getTransitScheduleFile().getParent());

        // read in transit-network, vehicles and schedule from osm
        Scenario scenarioFromOsmSchedule = new CreateScenarioFromOsmFile().run(ptInputParams.getOsmScheduleFile().toString());

        // read in transit-network, vehicles and schedule from gtfs
        Scenario scenarioFromGtfsSchedule = new CreateScenarioFromGtfs().run(ptInputParams.getGtfsFile().toString());

        // merge two transit networks into gtfs network
        MergeNetworks.merge(scenarioFromGtfsSchedule.getNetwork(), "", scenarioFromOsmSchedule.getNetwork());
        mergeSchedules(scenarioFromGtfsSchedule.getTransitSchedule(), scenarioFromOsmSchedule.getTransitSchedule());
        mergeVehicles(scenarioFromGtfsSchedule.getTransitVehicles(), scenarioFromOsmSchedule.getTransitVehicles());

        new VehicleWriterV1(scenarioFromGtfsSchedule.getTransitVehicles())
                .writeFile(ptOutputParams.getTransitVehiclesFile().toString());
        new TransitScheduleWriterV2(scenarioFromGtfsSchedule.getTransitSchedule())
                .write(ptOutputParams.getTransitScheduleFile().toString());

        // create the network from scratch
        NetworkCreator creator = new NetworkCreator.Builder()
                .setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
                .setSvnDir(arguments.svnDir)
                .withByciclePaths()
                .withOsmFilter(new FineNetworkFilter(networkInputParams.getInputNetworkShapeFilter()))
                .withCleaningModes(TransportMode.car, TransportMode.ride, TransportMode.bike)
                .withRideOnCarLinks()
                .build();

        Network network = creator.createNetwork();

        logger.info("merge transit networks and car/ride/bike network");
        MergeNetworks.merge(network, "", scenarioFromGtfsSchedule.getNetwork());

        logger.info("Writing network to: " + networkOutputParams.getOutputNetworkDir().resolve(SUBDIR));
        new NetworkWriter(network).write(outputNetwork.toString());

        // create long term counts
        Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
        NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 50025L)
                .build();
        Map<String, Counts<Link>> longTermCounts = longTermCountsCreator.run();

        // create short term counts
        NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 5025L)
                .build();
        Map<String, Counts<Link>> shortTermCounts = shortTermCountsCreator.run();

        writeCounts(networkOutputParams, columnCombinations, longTermCounts, shortTermCounts);
    }

    private static void mergeSchedules(TransitSchedule schedule, TransitSchedule toBeMerged) {
        toBeMerged.getFacilities().values().forEach(schedule::addStopFacility);
        toBeMerged.getTransitLines().values().forEach(schedule::addTransitLine);
    }

    private static void mergeVehicles(Vehicles vehicles, Vehicles toBeMerged) {
        toBeMerged.getVehicleTypes().values().forEach(vehicles::addVehicleType);
        toBeMerged.getVehicles().values().forEach(vehicles::addVehicle);
    }

    @SuppressWarnings("Duplicates")
    @SafeVarargs
    private static void writeCounts(NetworkOutput output, Set<String> columnCombinations, Map<String, Counts<Link>>... countsMaps) {

        // create a separate counts file for each column combination
        // each counts file contains all counts long term and short term count stations
        columnCombinations.forEach(combination -> {
            CombinedCountsWriter<Link> writer = new CombinedCountsWriter<>();
            Arrays.stream(countsMaps).forEach(map -> writer.addCounts(map.get(combination)));
            logger.info("writing counts to folder: " + output.getOutputNetworkDir().resolve(SUBDIR).toString());
            writer.write(output.getOutputNetworkDir().resolve(SUBDIR).resolve(FILE_PREFIX + "_counts_" + combination + ".xml").toString());
        });
    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true,
                description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
        private String svnDir;
    }
}
