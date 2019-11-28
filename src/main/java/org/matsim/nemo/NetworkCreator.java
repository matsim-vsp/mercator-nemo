package org.matsim.nemo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.bicycle.BicycleUtils;
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
import org.matsim.nemo.counts.CountsInput;
import org.matsim.osmNetworkReader.LinkProperties;
import org.matsim.osmNetworkReader.OsmTags;
import org.matsim.osmNetworkReader.SupersonicOsmNetworkReader;

import java.util.*;
import java.util.stream.Collectors;


class NetworkCreator {

    private static final double BIKE_PCU = 0.25;
    private Set<String> bicycleNotAllowed = new HashSet<>(Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link"));
    private static Logger logger = Logger.getLogger(NetworkCreator.class);

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
        // OsmNetworkReader networkReader = createNetworkReader(network, nodeIdsToKeep);
        // networkReader.parse(input.getInputOsmFile());

        SupersonicOsmNetworkReader networkReader = createSupersonicReader(network, nodeIdsToKeep);
        networkReader.read(input.getInputOsmFile());

        logger.info("validate network before cleaning");
      /*  validateParsedNetwork(network, nodeIdsToKeep);
        cleanNetwork(network);

        logger.info("validate network after cleaning");
        validateParsedNetwork(network, nodeIdsToKeep);
*/
        return network;
    }

    private Network createEmptyNetwork() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        return scenario.getNetwork();
    }

    private SupersonicOsmNetworkReader createSupersonicReader(Network network, Set<Long> nodeIdsToKeep) {

        return SupersonicOsmNetworkReader.builder()
                .network(network)
                .coordinateTransformation(this.transformation)
                .linkFilter(osmFilter::coordInFilter)
                .preserveNodeWithId(nodeIdsToKeep::contains)
                //.overridingLinkProperties(createBicycleLinkProperties())
                .afterLinkCreated(this::addBicyclePropertiesAndRideToLink)
                .build();

    }

    private void addBicyclePropertiesAndRideToLink(Link link, Map<String, String> tags, boolean isInverse) {

        // add ride mode to all car streets
        HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
        if (allowedModes.contains(TransportMode.car))
            allowedModes.add(TransportMode.ride);

        // add bike mode to most streets
        String highwayType = tags.get(OsmTags.HIGHWAY);
        if (!bicycleNotAllowed.contains(highwayType)) {
            allowedModes.add(TransportMode.bike);

        }
        link.setAllowedModes(allowedModes);

        //do surface
        if (tags.containsKey(BicycleUtils.SURFACE)) {
            link.getAttributes().putAttribute(BicycleUtils.SURFACE, tags.get(BicycleUtils.SURFACE));
        } else if (highwayType.equals("primary") || highwayType.equals("primary_link") || highwayType.equals("secondary") || highwayType.equals("secondary_link")) {
            link.getAttributes().putAttribute(BicycleUtils.SURFACE, "asphalt");
        }

        // do smoothness
        if (tags.containsKey(BicycleUtils.SMOOTHNESS)) {
            link.getAttributes().putAttribute(BicycleUtils.SMOOTHNESS, tags.get(BicycleUtils.SMOOTHNESS));
        }

        // do infrastructure factor
        link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5);

        //TODO add reverse direction for bicylces if street is only one way. Not sure how to fit that into the model of the reader
    }

    private Map<String, LinkProperties> createBicycleLinkProperties() {

        Map<String, LinkProperties> result = new HashMap<>();
        result.put("track", new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false));
        result.put("cycleway", new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false));
        result.put("service", new LinkProperties(9, 1, 10 / 3.6, 100 * BIKE_PCU, false));
        result.put("footway", new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false));
        result.put("pedestrian", new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false));
        result.put("path", new LinkProperties(10, 1, 20 / 3.6, 600 * BIKE_PCU, false));
        result.put("steps", new LinkProperties(11, 1, 1 / 3.6, 50 * BIKE_PCU, false));
        return result;
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
		 * Set osm filter other than org.matsim.nemo.FineNetworkFilter
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
					osmFilter != null ? osmFilter : new FineNetworkFilter(input.getInputNetworkShapeFilter()),
                    transformation,
                    cleaningModes
            );
        }
    }
}
