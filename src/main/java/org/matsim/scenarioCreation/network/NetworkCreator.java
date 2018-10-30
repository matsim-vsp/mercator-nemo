package org.matsim.scenarioCreation.network;


import lombok.AllArgsConstructor;
import lombok.val;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.bicycle.network.BicycleOsmNetworkReaderV2;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
class NetworkCreator {

    private static final double BIKE_PCU = 0.25;
    private static Logger logger = LoggerFactory.getLogger(NetworkCreator.class);

    private final NetworkInput input;
    private final NetworkOutput output;
    private final boolean withByciclePaths;
    private OsmNetworkReader.OsmFilter osmFilter;
    private CoordinateTransformation transformation;

    Network createNetwork() {

        val network = createEmptyNetwork();
        val nodeIdsToKeep = readNodeIds(Arrays.asList(input.getInputLongtermCountNodesMapping(), input.getInputShorttermCountMapping()));
        val networkReader = createNetworkReader(network, nodeIdsToKeep);
        networkReader.parse(input.getInputOsmFile());

        logger.info("validate network before cleaning");
        validateParsedNetwork(network, nodeIdsToKeep);
        cleanNetwork(network);

        logger.info("validate network after cleaning");
        validateParsedNetwork(network, nodeIdsToKeep);
        return network;
    }

    private Network createEmptyNetwork() {
        val config = ConfigUtils.createConfig();
        val scenario = ScenarioUtils.createScenario(config);
        return scenario.getNetwork();
    }

    private OsmNetworkReader createNetworkReader(Network network, Set<Long> nodeIdsToKeep) {
        OsmNetworkReader result;
        if (this.withByciclePaths) {
            result = new BicycleOsmNetworkReaderV2(
                    network, transformation, null, true,
                    TransportMode.bike, BIKE_PCU, true);
        } else {
            result = new OsmNetworkReader(network, transformation, true, true);
        }
        result.setKeepPaths(false);
        result.setNodeIDsToKeep(nodeIdsToKeep);
        result.addOsmFilter(getOsmFilterOrDefault());
        return result;
    }

    private OsmNetworkReader.OsmFilter getOsmFilterOrDefault() {
        if (osmFilter == null) {
            osmFilter = new NemoOsmFilter(input.getInputNetworkShapeFilter());
        }
        return osmFilter;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void validateParsedNetwork(Network network, Set<Long> nodeIdsToKeep) {

        nodeIdsToKeep.forEach(id -> {
            if (!network.getNodes().containsKey(id))
                logger.error("COULD NOT FIND NODE: " + id + " IN THE NETWORK");
        });
    }

    private void cleanNetwork(Network network) {
        new NetworkCleaner().run(network);
    }

    private Set<Long> readNodeIds(List<String> listOfCSVFiles) {

        val config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[]{";"});
        logger.info("start reading osm node ids of counts");

        return listOfCSVFiles.stream()
                .map(path -> parseCSVFile(path, config))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<Long> parseCSVFile(String path, TabularFileParserConfig config) {

        config.setFileName(path);
        final Set<Long> result = new HashSet<>();
        new TabularFileParser().parse(config, row -> {
            try {
                long first = Long.parseLong(row[1]);
                long second = Long.parseLong(row[2]);

                result.add(first);
                result.add(second);
            } catch (NumberFormatException e) {
                logger.info("reading counts with header: " + row[0] + ", " + row[1] + ", " + row[2]);
            }
        });
        return result;
    }


    public static class Builder {

        private String svnDir;
        private OsmNetworkReader.OsmFilter osmFilter;
        private boolean withByciclePaths = false;
        private CoordinateTransformation transformation;

        public Builder setSvnDir(String svnDir) {
            this.svnDir = svnDir;
            return this;
        }

        public Builder setOsmFilter(OsmNetworkReader.OsmFilter filter) {
            this.osmFilter = filter;
            return this;
        }

        public Builder setNetworkCoordinateSystem(String networkCoordinateSystem) {
            this.transformation =
                    TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, networkCoordinateSystem);
            return this;
        }

        public Builder withByciclePaths() {
            this.withByciclePaths = true;
            return this;
        }

        public NetworkCreator build() {
            return new NetworkCreator(
                    new NetworkInput(svnDir),
                    new NetworkOutput(svnDir),
                    withByciclePaths,
                    osmFilter,
                    transformation
            );
        }
    }
}
