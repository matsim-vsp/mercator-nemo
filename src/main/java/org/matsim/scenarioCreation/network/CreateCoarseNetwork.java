package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class CreateCoarseNetwork {

    //	dates are included in aggregation									year, month, dayOfMonth
    private final static LocalDate firstDayOfDataAggregation = LocalDate.of(2014, 1, 1);
    private final static LocalDate lastDayOfDataAggregation = LocalDate.of(2016, 12, 31);
    public static Logger logger = LoggerFactory.getLogger(CreateCoarseNetwork.class);
    private static String INPUT_OSM_FILE;
    private static String INPUT_LONGTERM_COUNT_DATA_ROOT_DIR;
    private static String INPUT_LONGTERM_COUNT_NODES_MAPPING;
    private static String INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR;
    private static String INPUT_SHORTTERM_COUNT_MAPPING;
    private static String INPUT_NETWORK_SHAPE_FILTER;
    private static String OUTPUT_DIR;
    private static String OUTPUT_COUNTS;

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);


    }

    private static void setConstants(String svnDir) {

        INPUT_OSM_FILE = svnDir + "/projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
        INPUT_LONGTERM_COUNT_DATA_ROOT_DIR = svnDir + "/projects/nemo_mercator/data/original_files/counts_rohdaten/dauerzaehlstellen";
        INPUT_LONGTERM_COUNT_NODES_MAPPING = svnDir + "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv";
        INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR = svnDir + "/projects/nemo_mercator/data/original_files/counts_rohdaten/verkehrszaehlung_2015/complete_Data";
        INPUT_SHORTTERM_COUNT_MAPPING = svnDir + "/projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv";
        INPUT_NETWORK_SHAPE_FILTER = svnDir + "/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";

        OUTPUT_DIR = svnDir + "/projets/nemo_mercartor/data/matsim_input/network";
        OUTPUT_COUNTS = svnDir + "/projects/nemo_mercartor/data/matsim_input/counts/car_counts.xml";
    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true)
        private String svnDir;
    }
}
