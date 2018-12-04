package org.matsim.scenarioCreation.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.Counts;
import org.matsim.scenarioCreation.counts.NemoLongTermCountsCreator;
import org.matsim.scenarioCreation.counts.NemoShortTermCountsCreator;
import org.matsim.scenarioCreation.counts.RawDataVehicleTypes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkAndCountsCreator {

    private NetworkCreator networkCreator;
    private String svnDir;

    private Network networkResult;
    private Map<String, Counts<Link>> shortTermCounts;
    private Map<String, Counts<Link>> longTermCounts;

    public NetworkAndCountsCreator(String svnDir, NetworkCreator networkCreator) {
        this.svnDir = svnDir;
        this.networkCreator = networkCreator;
    }

    public Network getNetworkResult() {
        return networkResult;
    }

    public Map<String, Counts<Link>> getShortTermCounts() {
        return shortTermCounts;
    }

    public Map<String, Counts<Link>> getLongTermCounts() {
        return longTermCounts;
    }

    public void generateNetworkAndCounts() {

        networkResult = networkCreator.createNetwork();

        //create counts
        // create long term counts
        Set<String> columnCombinations = new HashSet<>(Collections.singletonList(RawDataVehicleTypes.Pkw.toString()));
        NemoLongTermCountsCreator longTermCountsCreator = new NemoLongTermCountsCreator.Builder()
                .setSvnDir(svnDir)
                .withNetwork(networkResult)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 50025L)
                .build();
        longTermCounts = longTermCountsCreator.run();

        // create short term counts
        NemoShortTermCountsCreator shortTermCountsCreator = new NemoShortTermCountsCreator.Builder()
                .setSvnDir(svnDir)
                .withNetwork(networkResult)
                .withColumnCombinations(columnCombinations)
                .withStationIdsToOmit(5002L, 5025L)
                .build();
        shortTermCounts = shortTermCountsCreator.run();
    }
}
