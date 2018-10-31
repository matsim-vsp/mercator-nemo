package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.val;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.counts.Counts;
import org.matsim.scenarioCreation.counts.CombinedCountsWriter;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;
import org.matsim.util.NEMOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class CreateCoarseNetworkAndCarCounts {

    private static final String SUBDIR = "coarse";
    public static Logger logger = LoggerFactory.getLogger(CreateCoarseNetworkAndCarCounts.class);

    public static void main(String[] args) throws IOException {

        // parse input variables
        val arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        val outputParams = new NetworkOutput(arguments.svnDir);

        //ensure all output directories are present
        Files.createDirectories(outputParams.getOutputNetworkDir().resolve(SUBDIR));

        // create the network from scratch with default setting of network creator
        val creator = new NetworkCreator.Builder()
                .setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
                .setSvnDir(arguments.svnDir)
                .build();

        val network = creator.createNetwork();
        logger.info("Writing network to: " + outputParams.getOutputNetworkDir().resolve(SUBDIR));
        new NetworkWriter(network).write(outputParams.getOutputNetworkDir().resolve(SUBDIR).resolve("nemo_coarse_network.xml.gz").toString());

        // create long term counts
        Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
        val longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 50025L)
                .build();
        val longTermCounts = longTermCountsCreator.run();

        // create short term counts
        val shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 5025L)
                .build();
        val shortTermCounts = shortTermCountsCreator.run();

        writeCounts(outputParams, columnCombinations, longTermCounts, shortTermCounts);
    }

    @SafeVarargs
    private static void writeCounts(NetworkOutput output, Set<String> columnCombinations, Map<String, Counts<Link>>... countsMaps) {

        // create a separate counts file for each column combination
        // each counts file contains all counts long term and short term count stations
        columnCombinations.forEach(combination -> {
            val writer = new CombinedCountsWriter<Link>();
            Arrays.stream(countsMaps).forEach(map -> writer.addCounts(map.get(combination)));
            writer.write(output.getOutputNetworkDir().resolve(SUBDIR).resolve("nemo_coarse_network_counts_" + combination + ".xml").toString());
        });
    }


    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true,
                description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
        private String svnDir;
    }
}
