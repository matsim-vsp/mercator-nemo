package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.val;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.util.NEMOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class CreateCoarseNetwork {

    private static final String OUTPUT_NETWORK = "/projects/nemo_mercator/data/matsim_input/network";
    //	dates are included in aggregation									year, month, dayOfMonth
    private final static LocalDate firstDayOfDataAggregation = LocalDate.of(2014, 1, 1);
    private final static LocalDate lastDayOfDataAggregation = LocalDate.of(2016, 12, 31);
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


    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true)
        private String svnDir;
    }
}
