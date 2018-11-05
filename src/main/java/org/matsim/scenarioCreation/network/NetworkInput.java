package org.matsim.scenarioCreation.network;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class NetworkInput {

    private static final String INPUT_OSM_FILE = "/projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
    private static final String INPUT_NETWORK_SHAPE_FILTER = "/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";

    private String svnDir;

    String getInputOsmFile() {
        return svnDir + INPUT_OSM_FILE;
    }

    String getInputNetworkShapeFilter() {
        return svnDir + INPUT_NETWORK_SHAPE_FILTER;
    }
}
