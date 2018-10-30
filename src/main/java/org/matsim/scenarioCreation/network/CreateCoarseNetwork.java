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

import java.util.*;

public class CreateCoarseNetwork {

    private static final String OUTPUT_NETWORK = "/projects/nemo_mercator/data/matsim_input/network/nemo_network_coarse.xml.gz";
    //	dates are included in aggregation									year, month, dayOfMonth
    //private final static LocalDate firstDayOfDataAggregation = LocalDate.of(2014, 1, 1);
    //private final static LocalDate lastDayOfDataAggregation = LocalDate.of(2016, 12, 31);
    public static Logger logger = LoggerFactory.getLogger(CreateCoarseNetwork.class);

    public static void main(String[] args) {

        val arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        val creator = new NetworkCreator.Builder()
                .setNetworkCoordinateSystem(NEMOUtils.NEMO_EPSG)
                .setSvnDir(arguments.svnDir)
                .build();

        val network = creator.createNetwork();
        logger.info("Writing network to: " + arguments.svnDir + OUTPUT_NETWORK);
        new NetworkWriter(network).write(arguments.svnDir + OUTPUT_NETWORK);

        Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
        val longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombination(columnCombinations)
                .withStationIdsToOmit(5002L, 50025L)
                .build();
        val longTermCounts = longTermCountsCreator.run();

        val shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
                .setSvnDir(arguments.svnDir)
                .withNetwork(network)
                .withColumnCombination(columnCombinations)
                .withStationIdsToOmit(5002L, 5025L)
                .build();
        val shortTermCounts = shortTermCountsCreator.run();

        writeCounts(new NetworkOutput(arguments.svnDir), columnCombinations, longTermCounts, shortTermCounts);
    }

    @SafeVarargs
    private static void writeCounts(NetworkOutput output, Set<String> columnCombinations, Map<String, Counts<Link>>... countsMaps) {

        columnCombinations.forEach(combination -> {
            val writer = new CombinedCountsWriter<Link>();
            Arrays.stream(countsMaps).forEach(map -> writer.addCounts(map.get(combination)));
            writer.write(output.getOutputCountsDir() + "Counts_coarseNetwork_" + combination + "xml");
        });
    }


    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true)
        private String svnDir;
    }
}
