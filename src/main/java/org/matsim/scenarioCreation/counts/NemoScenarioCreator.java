/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.scenarioCreation.counts;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author tschlenther
 *
 */
	class NemoScenarioCreator {

	private final static String INPUT_OSMFILE = "data/input/counts/mapmatching/network/allWaysNRW.osm";
	
	private final static String INPUT_LONGTERM_COUNT_DATA_ROOT_DIR = "data/input/counts/rohdaten/dauerzaehlstellen";
	private final static String INPUT_COUNT_NODES_MAPPING_CSV= "data/input/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv";
	
	private final static String INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR = "data/input/counts/rohdaten/verkehrszaehlung_2015/complete_Data";
//	private final static String INPUT_SHORTTERM_COUNT_MAPPING_CSV = "data/input/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-Master.csv";
	private final static String INPUT_SHORTTERM_COUNT_MAPPING_CSV = "data/input/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv";
	
	private final static String INPUT_NETWORK = "data/input/network/allWaysNRW/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz";	// a network can be read in, in case it already exists, so network creation can be skipped or only simplifying and cleaning can be performed
	private final static String INPUT_NETWORK_SHAPE_FILTER = "data/cemdap_input/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";
	
	private final static boolean doSimplify = false;
	private final static boolean doCleaning = true;
	private final static String networkCoordinateSystem = "EPSG:25832";
	
	private static String OUTPUT_DIR_NETWORK = "data/input/network/allWaysNRW/";
	private static String OUTPUT_PREFIX_NETWORK = "";
	private static String OUTPUT_COUNTS_DIR = "data/input/counts/";
	
	//	dates are included in aggregation									year, month, dayOfMonth
	private final static LocalDate firstDayOfDataAggregation = LocalDate.of(2014, 1, 1);
	private final static LocalDate lastDayOfDataAggregation = LocalDate.of(2016, 12, 31);
	
	private static List<LocalDate> datesToIgnore = new ArrayList<LocalDate>();
	

	/**
	 * @param args
	 */
	

	
	public static void main(String[] args) {
		setOutputPaths();
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		/*
		 * create the network, use NemoNetworkCreator if new network is needed
		 * 
		 * 1) create new network, parsing from an osm file
		 * 2) work with an existing network 
		 *		a) keep network as it is and only parse it in
		 *		b) parse given network, give and give it to NemoNetworkCreator so it can simplify or clean it
		 */
		
		//create new network (1)
//		NemoNetworkCreator netCreator = new NemoNetworkCreator(INPUT_OSMFILE,
//				Arrays.asList(INPUT_COUNT_NODES_MAPPING_CSV,INPUT_SHORTTERM_COUNT_MAPPING_CSV),networkCoordinateSystem,OUTPUT_NETWORK,"Nemo");
//		netCreator.setShapeFileToFilter(INPUT_NETWORK_SHAPE_FILTER);
//		network = netCreator.createNetwork(doSimplify,doCleaning);
		
		//working with existing network (2)
			//a)
		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		netReader.readFile(INPUT_NETWORK);
			//b)
//		NemoNetworkCreator netCreator = new NemoNetworkCreator(network,INPUT_OSMFILE,
//				Arrays.asList(INPUT_COUNT_NODES_MAPPING_CSV,INPUT_SHORTTERM_COUNT_MAPPING_CSV),networkCoordinateSystem,OUTPUT_DIR_NETWORK,OUTPUT_PREFIX_NETWORK);

		
		/*	specify which counts files you want to generate:
		 * 	the raw data contains different columns, they are to be found in the RawDataVehicleTypes enum
		 * 	you can hand over the count creator an array
		 * 		- for each element, one count file is generated
		 * 		- columns can be summed up, e.g. you can aggregate motorbikes and bicycles by saying "Mot+Rad"
		 * 			=>always use ";" to connect two vehicle types		!!!
		 * 		- you can relate one column to several combinations, see example below
		 * 
		 * 	in this example, 4 count files would be generated, the third would contain the averages of the sum of cars and bikes
		 * 
		 * String[] combinations = new String[4];
		 * combinations[0] = RawDataVehicleTypes.Pkw.toString();
		 * combinations[1] = RawDataVehicleTypes.Rad.toString();
		 * combinations[2] = RawDataVehicleTypes.Pkw.toString() + ";" + RawDataVehicleTypes.Rad.toString();
		 * combinations[3] = RawDataVehicleTypes.SV.toString();
		 */
		
		String[] combinations = new String[4];
		combinations[0] = RawDataVehicleTypes.Pkw.toString();
		combinations[1] = RawDataVehicleTypes.Rad.toString();
		combinations[2] = RawDataVehicleTypes.SV.toString();
		combinations[3] = RawDataVehicleTypes.KFZ.toString();
		
		//instantiate NemoCountsCreator
		NemoCountsCreator countCreator = new NemoCountsCreator(combinations, network, INPUT_LONGTERM_COUNT_DATA_ROOT_DIR, INPUT_COUNT_NODES_MAPPING_CSV, OUTPUT_COUNTS_DIR + "longTerm/");
		
		/*specify analysis dates.. any data before firstDayOfDataAggregation and after lastDayOfDataAggregation are ignored.
		you can specify a list of ignored dates (e.g. school holidays) , but his will only have an effect on long term stations!! **/
		countCreator.setFirstDayOfAnalysis(firstDayOfDataAggregation);
		countCreator.setLastDayOfAnalysis(lastDayOfDataAggregation);
		countCreator.setDatesToIgnore(datesToIgnore);
		
		/*if you want to omit specific count data, you can add a list of their station numbers (see rohdaten)**/
//		countCreator.setCountingStationsToOmit(new ArrayList<Long>());
//		countCreator.addToStationsToOmit(lll);
		
		/*specify which months to analyze by setting monthRange_min and monthRange_max. 1 stands for january, 12 for december*/
//		countCreator.setMonthRange_min(monthRange_min);
		
		/*specify which weekdays you want to analyze by modifiying weekRange_min and weekRange_max. 1 stands for monday, 7 stands for sunday*/
//		countCreator.setWeekRangeMax(weekRange_max);
		
		
		countCreator.run();
		
		NemoShortTermCountsCreator shortTermCounts = new NemoShortTermCountsCreator(combinations, network, INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR ,
				INPUT_SHORTTERM_COUNT_MAPPING_CSV, OUTPUT_COUNTS_DIR + "shortTerm/", 2011, 2015);
		
		//give the shortTermClaculator the aggregated long term data so it can dump out everything together
		shortTermCounts.setCountsPerColumnCombinationMap(countCreator.getCountsPerColumnCombinationMap());
		shortTermCounts.setDatesToIgnore(datesToIgnore);
		
		//remember to set week day range again!
//		shortTermCounts.setWeekRangeMax(weekRange_max);
		
		
		shortTermCounts.run();
		shortTermCounts.setOutputPath(OUTPUT_COUNTS_DIR);
		shortTermCounts.writeOutput("allCounts");
	}

	
	private static void setOutputPaths(){
		SimpleDateFormat logTime = new SimpleDateFormat("ddMMyyyy");
        String creationTime = logTime.format(Calendar.getInstance().getTime());
		OUTPUT_COUNTS_DIR += creationTime + "/"; 
		OUTPUT_PREFIX_NETWORK += "Nemo_Network_" + creationTime;
		if(doSimplify) OUTPUT_PREFIX_NETWORK += "_simpl";
		if(doCleaning) OUTPUT_PREFIX_NETWORK += "_clean";
		OUTPUT_PREFIX_NETWORK += "_" + networkCoordinateSystem.replace(":", "_");
		
	}
	
}
