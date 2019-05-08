package org.matsim.nemo;

public class NetworkInput {

    private static final String INPUT_OSM_FILE = "/projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
    private static final String INPUT_NETWORK_SHAPE_FILTER = "/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";
    private static final String INPUT_BIKE_HIGHWAY_NETWORK = "/projects/nemo_mercator/data/original_files/BikeWays/Network_EMSCHERWEG_AND_RSV.xml";
    private static final String INPUT_BIKE_HIGHWAY_NETWORK_WITH_BRIDGE = "/projects/nemo_mercator/data/original_files/BikeWays/Network_RSV_AND_EMSCHERWEG_WITH_IK-BRIDGE.xml";

    private String svnDir;

    public NetworkInput(String svnDir) {
        this.svnDir = svnDir;
    }

    String getInputOsmFile() {
        return svnDir + INPUT_OSM_FILE;
    }

    String getInputNetworkShapeFilter() {
        return svnDir + INPUT_NETWORK_SHAPE_FILTER;
    }

    String getInputBikeHighwayNetwork() {
        return svnDir + INPUT_BIKE_HIGHWAY_NETWORK;
    }

    String getInputBikeHighwayNetworkWithBridge() {
        return svnDir + INPUT_BIKE_HIGHWAY_NETWORK_WITH_BRIDGE;
    }
}
