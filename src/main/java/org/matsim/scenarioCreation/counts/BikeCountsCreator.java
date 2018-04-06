/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

/**
 * @author tschlenther
 * 
 * this class is created based on the assumption that input data is already aggregated in two files,
 * one containing the count data and one containing the localisation of counting stations.
 * both files are assumed to be csv files, with ';' as column separator
 */
public class BikeCountsCreator {

	//----------------------INPUT FILES---------------------------------------------
	private static final String COUNT_DATA_FILE = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/data/matsim_input/zz_archive/bike_counts/CountData.csv";
	private static final String COUNT_LOCALISATION_FILE = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/data/matsim_input/zz_archive/bike_counts/CountLocations_test.csv";
	private static final String NETWORK_FILE = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/data/matsim_input/2018-03-13_RuhrDetailedNet_unCleaned/detailedRuhr_Network_15032018unfiltered_network_bike.xml";
//	private static final String NETWORK_FILE = "";
	
	//----------------------OUTPUT FILES---------------------------------------------
	private static String outputPath = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/data/matsim_input/bikeCounts/";
	
	//-------------------------------------------------------------------------------
	protected Logger log = Logger.getLogger(this.getClass().getName());
	private File countDataFile;
	private File countLocalisationFile;
	private final Network network;
	private Map<String,HourlyCountData> countDataMap = new HashMap<String,HourlyCountData>();
	private Map<String,Id<Link>> linkIDsOfCounts = new HashMap<String,Id<Link>>();
	private String output;
	/**
	 * 
	 */
	BikeCountsCreator(String inputDataFile, String inputLocalisationFile, String outputPath) {
		countDataFile = new File(inputDataFile);
		countLocalisationFile = new File(inputLocalisationFile);
		if(!countDataFile.exists() || !countLocalisationFile.exists()){
			throw new IllegalArgumentException();
		}
		network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		File outputDir = new File(outputPath);
		if (!outputDir.exists()){
			outputDir.mkdirs();
		}
		this.output = outputDir.getAbsolutePath() + "/";
	}

	/**
	 * @param networkFile
	 */
	private void readNetwork(String networkFile) {
		log.info("Start reading the network...");
		MatsimNetworkReader reader = new MatsimNetworkReader(network);
		reader.readFile(networkFile);
		log.info("finished reading the network...");
	}
	
	public static void main(String[] args){
		BikeCountsCreator creator = new BikeCountsCreator(COUNT_DATA_FILE, COUNT_LOCALISATION_FILE, outputPath);
		creator.initializeLogger();
		creator.readNetwork(NETWORK_FILE);
		creator.readCountData();
		creator.readCountLocation();
		creator.convertDataToMATSimCounts();
		creator.finish();
	}
	
	private void convertDataToMATSimCounts() {
		log.info("start conversion of input data into matsim counts");
		String description = "bike counts for nemo project. created: " + LocalDate.now();
		Counts container = new Counts();
		container.setDescription(description);
		container.setYear(LocalDate.now().getYear());
		
		List<String> allDataStations = new ArrayList<String>();
		allDataStations.addAll(this.countDataMap.keySet());
		int cnt = 0;
		for(String stationID : this.countDataMap.keySet()){
			cnt ++;
			if(cnt % 10 == 0){
				log.info("converting station nr " + cnt);
			}
			
			HourlyCountData dataObject = this.countDataMap.get(stationID);
			Id<Link> linkIDDirectionOne = this.linkIDsOfCounts.remove(stationID + "_R1");
			Id<Link> linkIDDirectionTwo = this.linkIDsOfCounts.remove(stationID + "_R2");
	
			if(dataObject == null){
				log.severe("can not access the hourlycountdata... the countID is" + stationID );
				throw new NullPointerException();
			}
			if(linkIDDirectionOne == null || linkIDDirectionTwo== null) {
				continue;
			}
				
				Count<Link> countDirOne = null;
				Count<Link> countDirTwo = null;
			try{
				if(container==null) System.out.println("ACHTUNG CONTAINER IST NULL");
				
					countDirOne = container.createAndAddCount(linkIDDirectionOne, dataObject.getId() + "_R1" );
					countDirTwo = container.createAndAddCount(linkIDDirectionTwo, dataObject.getId() + "_R2" );

				for(int i = 1; i < 25; i++) {
					Double valueDirOne = dataObject.getR1Values().get(i);
					Double valueDirTwo = dataObject.getR2Values().get(i);

					if(valueDirOne == null){
						String problem = "station " + stationID + " has a non-valid entry for hour " + i +" in direction one. Please check this. The value in the count file is set to -1."; 
						log.warning( problem + " Error occured at creation nr " + cnt);
						valueDirOne = -1.;
					}
					if(valueDirTwo == null){
						String problem = "station " + stationID + " has a non-valid entry for hour " + i +" in direction two. Please check this. The value in the count file is set to -1."; 
						log.warning( problem + " Error occured at creation nr " + cnt);
						valueDirTwo = -1.0;
					}
						if(countDirOne != null)	countDirOne.createVolume(i, Math.ceil(valueDirOne));
						if(countDirTwo != null) countDirTwo.createVolume(i, Math.ceil(valueDirTwo));
				}
				
				allDataStations.remove(stationID);
				
			} catch(Exception e){
				e.printStackTrace();
				continue;
			}
		}
		//--------------statistics---------------------
		String msg = "---------------------------";
		if(!allDataStations.isEmpty()) {
					msg += "\n" + 	" number of stations for which data is available but no localisation entry was found: " + this.countDataMap.size() + 
					"\n" + 	"A list of their id's: \n";
			for(String id: allDataStations){
				msg += id + "\n";
			}
			log.info(msg);
		}
		if(!this.linkIDsOfCounts.isEmpty()) {
			msg = "a list of stations that were localised but no data was available:\n";
			for(String id : this.linkIDsOfCounts.keySet()) {
				msg += id + "\n";
			}
			log.info(msg);
		}
		//----------------write output---------------------
		log.info("finished conversion of data...");
		log.info("writing counts to " + this.output + "BikeCounts.xml");
		CountsWriter writer = new CountsWriter(container);
		writer.write(this.output + "BikeCounts_" + new SimpleDateFormat("ddMMyyyy").format(new Date()) + ".xml");
		log.info("finished writing counts file");
	}

	private void readCountLocation() {
	log.info("...start reading node id's from localisation input file");
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(countLocalisationFile.getAbsolutePath());
        
        CountLinkFinder linkFinder = new CountLinkFinder(network);
        new TabularFileParser().parse(config, new TabularFileHandler() {
        	private boolean header = true;
        	
			@Override
			public void startRow(String[] row) {
				if(!header){
					//direction one
					if(row.length >= 6){
						log.severe("station " + row[0] + " is commented like this in the map matching csv file: " + row[5]);
					}
					String stationID = row[0] + "_R1";
					Id<Link> countLinkID = findLinkId(stationID, row[1],row[2],linkFinder);
					linkIDsOfCounts.put(stationID, countLinkID);
					
					stationID = row[0] + "_R2";
					countLinkID = findLinkId(stationID, row[3],row[4],linkFinder);
					linkIDsOfCounts.put(stationID, countLinkID);
				}
				header = false;
			}
        });
        log.info("-----------------------------------------------------");
        log.info("read in " + linkIDsOfCounts.size() + " link-id's");
        log.info("number of node-id-mappings that were not directly connected by a link, but a path could be calculated: " + linkFinder.getNrOfFoundPaths());
        
        if(linkFinder.getNrOfFoundPaths() >= 1){
        	log.info("writing out a network file for visualisation that contains all reconstructed paths between the corresponding fromNodes and toNodes. The first link of a path has capacity of 0, the second capacity of 1 ....");
    		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HH_mm");
        	linkFinder.writeNetworkThatShowsAllFoundPaths(this.output + "visNetOfReconstructedPaths_" + format.format(Calendar.getInstance().getTime()) + ".xml");
        }
	}
	
	private Id<Link> findLinkId (String stationID, String fromNodeID, String toNodeID, CountLinkFinder linkFinder){
		Id<Link> countLinkID = null;

		Node fromNode = network.getNodes().get(Id.createNodeId(Long.parseLong(fromNodeID)));
		Node toNode = network.getNodes().get(Id.createNodeId(Long.parseLong(toNodeID)));
		if(fromNode == null){
			String problem = "could not find fromNode " + fromNodeID + ". station id= " + stationID;
			log.warning(problem);
			log.warning("setting a non-valid link id: " + "noFromNode"  + stationID);
			return Id.createLinkId("noFromNode_" + stationID);
		}
		if(toNode == null){
			String problem = "could not find toNode with id=" + toNodeID + ". station id=" + stationID;
			log.warning(problem);
			log.warning("setting a non-valid link id: " + "noToNode"  + stationID);
			return Id.createLinkId("noToNode_" + stationID);
		}
		for(Link outlink : fromNode.getOutLinks().values()){
			if(outlink.getToNode().getId().equals(toNode.getId())){
				countLinkID = outlink.getId();
			}
		}
		if(countLinkID == null){
			String problem;
			problem = "could not find a link directly leading from node " + fromNode.getId() + " to node " + toNode.getId();
			log.severe(problem);
			
			countLinkID = linkFinder.getFirstLinkOnTheWayFromNodeToNode(fromNode, toNode);
			if(countLinkID == null){
				problem = "COULD FIND NO PATH LEADING FROM NODE " + fromNode.getId() + " TO NODE " + toNode.getId();
				log.severe(problem);
				countLinkID = Id.createLinkId("pathCouldNotBeCreated_" + stationID);
			}
		}	
		return countLinkID;
	}

	private void readCountData() {
		log.info("start reading count data...");
		BufferedReader reader;
		try {
			reader = new BufferedReader( new InputStreamReader(new FileInputStream(countDataFile), "windows-1256"));
			
			//read header
			String currentLine = reader.readLine();
			
			// read data and store it in the HashMap
			while((currentLine = reader.readLine()) != null) {
				String[] lineArray = currentLine.split(";");
				int hour = Integer.parseInt(lineArray[1]);
				double valueDir1 = Double.parseDouble(lineArray[2]);
				double valueDir2 = Double.parseDouble(lineArray[3]);

				HourlyCountData data;
				if(this.countDataMap.containsKey(lineArray[0])){
					data = countDataMap.get(lineArray[0]);
				} else {
					 data = new HourlyCountData(lineArray[0], null);
				}
				data.computeAndSetVolume(true, hour, valueDir1);
				data.computeAndSetVolume(false, hour, valueDir2);
				
				countDataMap.put(lineArray[0], data);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("finished reading count data...");
	}

	private void initializeLogger(){
		 SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		 FileHandler fh = null;
		 ConsoleHandler ch = null;
	        try {
	        	fh = new FileHandler(this.output + "Log_"
	                + format.format(Calendar.getInstance().getTime()) + ".log");
	        	ch = new ConsoleHandler();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        Formatter formatter = new Formatter() {
	        	
	            @Override
	            public String format(LogRecord record) {
	                SimpleDateFormat logTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	                Calendar cal = new GregorianCalendar();
	                cal.setTimeInMillis(record.getMillis());
	                return ( record.getLevel() + " "
	                        + logTime.format(cal.getTime())
	                        + " || "
	                        + record.getSourceClassName().substring(
	                                record.getSourceClassName().lastIndexOf(".")+1,
	                                record.getSourceClassName().length())
	                        + "."
	                        + record.getSourceMethodName()
	                        + "() : "
	                        + record.getMessage() + "\n");
	            }
	        };
	        log.setUseParentHandlers(false);
	        fh.setFormatter(formatter);
	        ch.setFormatter(formatter);
	        log.addHandler(fh);
	        log.addHandler(ch);
	}
	
	private void finish() {
		
		log.warning("PLEASE BE AWARE OF THE FOLLOWING: the counts file might still contain some counts that could not be located during conversion process. \n"
				+ "if you want to carry on with this file using it as MATSim input, please use the CountsCleaner.java in combination with the given network!");
		log.info("...closing " + this.getClass().getName() + "...");
	}

}
