package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterBikeLinksFromNetwork {

    private static final String workingFolder = "/projects/nemo_mercator/data/matsim_input/2018-10-01_baseCase/";
    private static final String inputFileName = "detailedRuhr_Network_10072018filtered_network_GTFS_OSM.xml.gz";
    private static final String outputFileName = "network_ruhr_without-bike_with-pt.xml.gz";

	public static void main(String[] args) {

        Args arguments = new Args();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

		Network network = NetworkUtils.createNetwork();

        new MatsimNetworkReader(network).readFile(arguments.sharedSvnPath + workingFolder + inputFileName);

        System.out.println("Start filtering Network");
        List<Link> toRemove = network.getLinks().values().stream().filter((link) ->
                (!link.getAllowedModes().contains("car") && !link.getAllowedModes().contains("pt") && !link.getAllowedModes().contains("ride")))
                .collect(Collectors.toList());

        System.out.println(toRemove.size() + " links to remove");
        toRemove.forEach(link -> network.removeLink(link.getId()));
        new MultimodalNetworkCleaner(network).run(getModes());
        System.out.println("Start writing network.");
        new NetworkWriter(network).write(arguments.sharedSvnPath + workingFolder + outputFileName);

        System.out.println("Done. Exiting Program");
    }

    private static Set<String> getModes() {
        Set<String> modes = new HashSet<>();
        modes.add("car");
        modes.add("ride");
        modes.add("bike");
        return modes;
    }

    private static class Args {

        @Parameter(names = "-shared-svn")
        private String sharedSvnPath;
    }
}