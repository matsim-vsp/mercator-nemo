package org.matsim.scenarioCreation.counts;


public class CountsInput {

    private static final String INPUT_LONGTERM_COUNT_DATA_ROOT_DIR = "/projects/nemo_mercator/data/original_files/counts_rohdaten/dauerzaehlstellen";
    private static final String INPUT_LONGTERM_COUNT_NODES_MAPPING = "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv";
    private static final String INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR = "/projects/nemo_mercator/data/original_files/counts_rohdaten/verkehrszaehlung_2015/complete_Data";
    private static final String INPUT_SHORTTERM_COUNT_MAPPING = "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv";

    private String svnDir;

    public CountsInput(String svnDir) {
        this.svnDir = svnDir;
    }

    String getInputLongtermCountDataRootDir() {
        return svnDir + INPUT_LONGTERM_COUNT_DATA_ROOT_DIR;
    }

    public String getInputLongtermCountNodesMapping() {
        return svnDir + INPUT_LONGTERM_COUNT_NODES_MAPPING;
    }

    String getInputShorttermCountDataRootDir() {
        return svnDir + INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR;
    }

    public String getInputShorttermCountMapping() {
        return svnDir + INPUT_SHORTTERM_COUNT_MAPPING;
    }

}
