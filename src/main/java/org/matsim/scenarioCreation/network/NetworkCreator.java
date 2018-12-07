package org.matsim.scenarioCreation.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.bicycle.network.BicycleOsmNetworkReaderV2;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.scenarioCreation.counts.CountsInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


class NetworkCreator {

    private static final double BIKE_PCU = 0.25;
    private static Logger logger = LoggerFactory.getLogger(NetworkCreator.class);

    private final NetworkInput input;
    private final CountsInput countsInput;
    private final boolean withBicyclePaths;
    private final boolean withRideOnCarLinks;
    private final OsmNetworkReader.OsmFilter osmFilter;
    private final CoordinateTransformation transformation;
    private final Set<String> cleaningModes;

    private NetworkCreator(NetworkInput input, CountsInput countsInput, boolean withBicyclePaths, boolean withRideOnCarLinks,
                           OsmNetworkReader.OsmFilter osmFilter, CoordinateTransformation ct, Set<String> cleaningModes) {
        this.input = input;
        this.countsInput = countsInput;
        this.withBicyclePaths = withBicyclePaths;
        this.withRideOnCarLinks = withRideOnCarLinks;
        this.osmFilter = osmFilter;
        this.transformation = ct;
        this.cleaningModes = cleaningModes;
    }

    Network createNetwork() {

        Network network = createEmptyNetwork();
        Set<Long> nodeIdsToKeep = readNodeIds(Arrays.asList(countsInput.getInputLongtermCountNodesMapping(), countsInput.getInputShorttermCountMapping()));
        OsmNetworkReader networkReader = createNetworkReader(network, nodeIdsToKeep);
        networkReader.parse(input.getInputOsmFile());

        logger.info("validate network before cleaning");
        validateParsedNetwork(network, nodeIdsToKeep);
        cleanNetwork(network);

        logger.info("validate network after cleaning");
        validateParsedNetwork(network, nodeIdsToKeep);

        if (withRideOnCarLinks) {
            addRideOnCarLinks(network);
        }
        return network;
    }

    private Network createEmptyNetwork() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        return scenario.getNetwork();
    }

    private OsmNetworkReader createNetworkReader(Network network, Set<Long> nodeIdsToKeep) {
        OsmNetworkReader result;
        if (this.withBicyclePaths) {
            result = new BicycleOsmNetworkReaderV2(
                    network, transformation, null, true,
                    TransportMode.bike, BIKE_PCU, true);
        } else {
            result = new OsmNetworkReader(network, transformation, true, true);
        }
        result.setKeepPaths(false);
        result.setNodeIDsToKeep(nodeIdsToKeep);
        result.addOsmFilter(osmFilter);
        return result;
    }

    private void validateParsedNetwork(Network network, Set<Long> nodeIdsToKeep) {

        nodeIdsToKeep.forEach(id -> {
            if (!network.getNodes().containsKey(Id.createNodeId(id)))
                logger.error("COULD NOT FIND NODE: " + id + " IN THE NETWORK");
        });
    }

    private void cleanNetwork(Network network) {
        if (cleaningModes.isEmpty())
            new NetworkCleaner().run(network);
        else {
            cleaningModes.forEach(mode -> new MultimodalNetworkCleaner(network).run(new HashSet<>(Collections.singletonList(mode))));
        }
    }

    private void addRideOnCarLinks(Network network) {

        network.getLinks().values().stream().filter(link -> link.getAllowedModes().contains(TransportMode.car))
                .forEach(link -> {
                    Set<String> modes = new HashSet<>(link.getAllowedModes());
                    modes.add(TransportMode.ride);
                    link.setAllowedModes(modes);
                });
    }

    private Set<Long> readNodeIds(List<String> listOfCSVFiles) {

        TabularFileParserConfig config = new TabularFileParserConfig();
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
        private boolean withBicyclePaths = false;
        private boolean withRideOnCarLinks = false;
        private CoordinateTransformation transformation;
        private Set<String> cleaningModes = new HashSet<>();

        /**
         * @param svnDir Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder
         * @return Current Builder instance
         */
        Builder setSvnDir(String svnDir) {
            this.svnDir = svnDir;
            return this;
        }

        /**
         * Set osm filter other than org.matsim.scenarioCreation.network.NemoOsmFilter
         * @param filter only links filtered by this filter will be contained in the network
         * @return Current Builder instance
         */
        Builder withOsmFilter(OsmNetworkReader.OsmFilter filter) {
            this.osmFilter = filter;
            return this;
        }

        /**
         *
         * @param networkCoordinateSystem set coordinate system transformation like e.g. EPSG:25832
         * @return Current Builder instance
         */
        Builder setNetworkCoordinateSystem(String networkCoordinateSystem) {
            this.transformation =
                    TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, networkCoordinateSystem);
            return this;
        }

        /**
         * With this set BicycleOsmNetworkReaderV2 is used to parse the network. The default is OsmNetworkReader
         * @return Current Builder instance
         */
        Builder withByciclePaths() {
            this.withBicyclePaths = true;
            return this;
        }

        Builder withRideOnCarLinks() {
            this.withRideOnCarLinks = true;
            return this;
        }

        Builder withCleaningModes(String... modes) {
            this.cleaningModes = new HashSet<>(Arrays.asList(modes));
            return this;
        }

        /**
         * @return new instance of NetworkCreator
         */
        public NetworkCreator build() {

            NetworkInput input = new NetworkInput(svnDir);
            return new NetworkCreator(
                    input,
                    new CountsInput(svnDir),
                    withBicyclePaths,
                    withRideOnCarLinks,
                    osmFilter != null ? osmFilter : new NemoOsmFilter(input.getInputNetworkShapeFilter()),
                    transformation,
                    cleaningModes
            );
        }
    }
}
