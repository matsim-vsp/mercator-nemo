package org.matsim.scenarioCreation.network;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NetworkInput {

    private static final String INPUT_OSM_FILE = "/projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
    private static final String INPUT_LONGTERM_COUNT_DATA_ROOT_DIR = "/projects/nemo_mercator/data/original_files/counts_rohdaten/dauerzaehlstellen";
    private static final String INPUT_LONGTERM_COUNT_NODES_MAPPING = "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv";
    private static final String INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR = "/projects/nemo_mercator/data/original_files/counts_rohdaten/verkehrszaehlung_2015/complete_Data";
    private static final String INPUT_SHORTTERM_COUNT_MAPPING = "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv";
    private static final String INPUT_NETWORK_SHAPE_FILTER = "/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";

    private String svnDir;

    public String getInputOsmFile() {
        return svnDir + INPUT_OSM_FILE;
    }

    public String getInputLongtermCountDataRootDir() {
        return svnDir + INPUT_LONGTERM_COUNT_DATA_ROOT_DIR;
    }

    public String getInputLongtermCountNodesMapping() {
        return svnDir + INPUT_LONGTERM_COUNT_NODES_MAPPING;
    }

    public String getInputShorttermCountDataRootDir() {
        return svnDir + INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR;
    }

    public String getInputShorttermCountMapping() {
        return svnDir + INPUT_SHORTTERM_COUNT_MAPPING;
    }

    public String getInputNetworkShapeFilter() {
        return svnDir + INPUT_NETWORK_SHAPE_FILTER;
    }
}
