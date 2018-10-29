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

package org.matsim.scenarioCreation.network;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.bicycle.network.BicycleOsmNetworkReaderV2;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.OsmNetworkReader.OsmFilter;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.util.NEMOUtils;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author tschlenther
 *
 */
public class NemoNetworkCreator {
	
	private static final Logger log = Logger.getLogger(NemoNetworkCreator.class);

	private final String INPUT_OSMFILE ;
	private final List<String> INPUT_COUNT_NODE_MAPPINGS;
	private final String outputDir;
	private final String prefix;
	private final String networkCS ;

	//optional
	private boolean filterWithShape  = false;
	private String pathToShapeFileForFilter;
	private Network network = null;

	private double linkLengthMinThreshold = Double.POSITIVE_INFINITY;

	// configurable osm filter
	private OsmFilter osmFilter;
	// seperating creation and writing of network to allow other modifications to network outside this class. Amit Feb'18
	private String outnetworkPrefix ;
	private boolean includeBicyclePaths;
	
	public NemoNetworkCreator(String inputOSMFile, List<String> inputCountNodeMappingFiles, String networkCoordinateSystem, String outputDir, String prefix) {
		this.INPUT_OSMFILE = inputOSMFile;
		this.INPUT_COUNT_NODE_MAPPINGS = inputCountNodeMappingFiles;
		this.networkCS = networkCoordinateSystem;
		this.outputDir = outputDir.endsWith("/")?outputDir:outputDir+"/";
		this.prefix = prefix;
		this.outnetworkPrefix = prefix;
		initLogger();
		log.info("---set the coordinate system for network to be created to "+this.networkCS+" ---");
	}

	public NemoNetworkCreator(Network network, String inputOSMFile, List<String> inputCountNodeMappingFiles, String networkCoordinateSystem, String outputDir, String prefix){
		this (inputOSMFile, inputCountNodeMappingFiles, networkCoordinateSystem, outputDir, prefix);
		this.network = network;
		log.info("initial network was given, assuming it's coordinates are in "+networkCoordinateSystem);
	}

	public void setShapeFileToFilter(String pathToShapeFile){
		this.pathToShapeFileForFilter = pathToShapeFile;
		this.filterWithShape = true;
	}
	
	private void initLogger(){
		FileAppender fa = new FileAppender();
		fa.setFile(outputDir + prefix +"_LOG_"+NemoNetworkCreator.class.getSimpleName()+".txt");
		fa.setName("NemoNetworkCreator");
		fa.activateOptions();
		fa.setLayout(new PatternLayout(
				    "%d{dd MMM yyyy HH:mm:ss,SSS} %-4r [%t] %-5p %c %x - %m%n"));
	     log.addAppender(fa);
	}

	public static void main(String[] args) {
		String osmfile = "data/input/counts/mapmatching/network/allWaysNRW.osm";
		List<String> inputCountNodeMappingFiles = Arrays.asList("data/input/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv", "data/input/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv");
		String epsg = NEMOUtils.NEMO_EPSG;
		String prefix = "tertiaryNemo_Network_"+new SimpleDateFormat("ddMMyyyy").format(new Date());
		String outDir = "data/input/network/allWaysNRW/";

		NemoNetworkCreator nemoNetworkCreator = new NemoNetworkCreator(osmfile, inputCountNodeMappingFiles, epsg, outDir, prefix);
		nemoNetworkCreator.setShapeFileToFilter("data/cemdap_input/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp");
		nemoNetworkCreator.createNetwork(false, true);
		nemoNetworkCreator.writeNetwork();
	}

	public void setOSMFilter(OsmFilter osmFilter){
		this.osmFilter = osmFilter;
	}
	public void setIncludeBicyclePaths(boolean includeBicyclePaths){
		this.includeBicyclePaths = includeBicyclePaths;
	}

	/**
	 * Write network is removed from createAndWriteNetwork to allow other changes to network before writing.
	 */
	public void writeNetwork(){
		String outNetwork = this.outputDir+outnetworkPrefix+"_network.xml.gz";
		log.info("The network is written to " + outNetwork);
		new NetworkWriter(network).write(outNetwork);
		log.info("..finished..");
	}
	
	public void createNetwork(boolean doSimplify, boolean doCleaning){
		double bicyclePCU = 0.25;
		CoordinateTransformation ct =
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, networkCS);
		
		Set<Long> nodeIDsToKeep = readNodeIDs(INPUT_COUNT_NODE_MAPPINGS);

		if(this.network == null) {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			network = scenario.getNetwork();

			log.info("start parsing from osm file " + INPUT_OSMFILE);
	
			OsmNetworkReader networkReader;
			if (this.includeBicyclePaths) {
                networkReader = new BicycleOsmNetworkReaderV2(
                        network, ct, null, true, "bike", 0.25, true);
			} else {
				networkReader = new OsmNetworkReader(network,ct, true, true);
			}

			networkReader.setKeepPaths(false);
			networkReader.setNodeIDsToKeep(nodeIDsToKeep);
	
			if(filterWithShape){
				outnetworkPrefix += "filtered";
				if (this.osmFilter==null ){
					this.osmFilter = new NemoOsmFilter(pathToShapeFileForFilter);
				}
				networkReader.addOsmFilter(this.osmFilter);
			}
	
			networkReader.parse(INPUT_OSMFILE);
			log.info("finished parsing osm file");
		}	
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK BEFORE SIMPLIFYING AND CLEANING");
			}
		}
		
		if(doSimplify){
			outnetworkPrefix += "simplified";
			log.info("number of nodes before simplifying:" + network.getNodes().size());
			log.info("number of links before simplifying:" + network.getLinks().size());
			log.info("start simplifying the network");
			/*
			 * simplify network: merge links that are shorter than the given threshold
			 */

			NetworkSimplifier simp = new NetworkSimplifier();
			simp.setNodesNotToMerge(nodeIDsToKeep);
			simp.setMergeLinkStats(false);
			simp.run(network);
			
			log.info("checking if all count nodes are in the network..");
			for(Long id : nodeIDsToKeep){
				if(!network.getNodes().containsKey(Id.createNodeId(id))){
					log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER SIMPLIFYING");
				}
			}
		}
		if(doCleaning){
			outnetworkPrefix += "cleaned";
				/*
				 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
				 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
				 */
			log.info("number of nodes before cleaning:" + network.getNodes().size());
			log.info("number of links before cleaning:" + network.getLinks().size());
			log.info("attempt to clean the network");
			new NetworkCleaner().run(network);
		}
		
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER NETWORK CREATION");
			}
		}
	}

	/**
	 * sets the link length threshold for the internally used {@link NetworkSimplifier}
	 */
	public void setLinkLenghtMinThreshold(double minLinkLength){
		this.linkLengthMinThreshold = minLinkLength;
	}
	
	/**
	 * expects a path to a csv file that has the following structure: <br><br>
	 * 
	 * COUNT-ID;OSM_FROMNODE_ID;OSM_TONODE_ID <br><br>
	 * 
	 * It is assumed that the csv file contains a header line.
	 * Returns a set of all mentioned osm-node-ids.
	 * 
	 * @param listOfCSVFiles
	 * @return
	 */
	private Set<Long> readNodeIDs(List<String> listOfCSVFiles){
		final Set<Long> allNodeIDs = new HashSet<Long>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {";"});
	    
	    log.info("start reading osm node id's of counts");
	    for(String path : listOfCSVFiles){
	    	log.info("reading node id's from" + path);
	    	config.setFileName(path);	
	    	new TabularFileParser().parse(config, new TabularFileHandler() {
	    		boolean header = true;
	    		@Override
	    		public void startRow(String[] row) {
	    			if(!header){
	    				if( !(row[1].equals("") || row[2].equals("") ) ){
	    					allNodeIDs.add( Long.parseLong(row[1]));
	    					allNodeIDs.add( Long.parseLong(row[2]));
	    				}
	    			}
	    			header = false;				
	    		}
	    	});
	    }
	    return allNodeIDs;
	}
	
	public Network getNetwork(){
		return this.network;
	}
}
