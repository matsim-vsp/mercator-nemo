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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

/**
 * @author tschlenther
 *
 */
public class NemoLongTermCountsCreator {

	public static final Logger log = Logger.getLogger(NemoLongTermCountsCreator.class.getName());

	private final String outputPath;
	private final String pathToCountData;
	private final String pathToOSMMappingFile;
	final Network network;
	
	private LocalDate firstDayOfAnalysis = null;
	private LocalDate lastDayOfAnalysis = null;
	private final List<LocalDate> datesToIgnore = new ArrayList<LocalDate>();
	
	private int monthRange_min = 1;
	private int monthRange_max = 12;

	int weekRange_min = 1;
	int weekRange_max = 5;
	
	private static final boolean USE_DATA_WITH_LESS_THAN_9_VEHICLE_CLASSES = true;

	Map<String,Map<String,HourlyCountData>> countingStationsData  = new HashMap<String,Map<String,HourlyCountData>>();
	
//	protected Map<String,HourlyCountData> kfzCountingStationsData = new HashMap<String,HourlyCountData>();
//	protected Map<String,HourlyCountData> svCountingStationsData = new HashMap<String,HourlyCountData>();
	
	private Map<String,String> countingStationNames = new HashMap<String,String>();
	private Map<String,String> problemsPerCountingStation = new HashMap<String,String>();
	private List<String> notLocatedCountingStations = new ArrayList<String>();
	private final List<String> notMapMatchedStations = new ArrayList<String>();
	
	private Map<String,Id<Link>> linkIDsOfCountingStations = new HashMap<String,Id<Link>>();
	final List<Long> countingStationsToOmit = new ArrayList<Long>();

	//elements of this array specify which columns of input data to consider and which ones to sum up
	private final Map<String,Counts<Link>> countsPerColumnCombination ;
	

	final List<String> allNeededColumnHeaders = new ArrayList<String>();

	public NemoLongTermCountsCreator(Map<String, Counts<Link>> countsPerColumnCombination, Network network,
									 String pathToCountDataRootDirectory, String pathToCountStationToOSMNodesMappingFile, String outputPath) {
		this.countsPerColumnCombination = countsPerColumnCombination;
		this.network = network;
		this.outputPath = outputPath;
		this.pathToCountData = pathToCountDataRootDirectory;
		this.pathToOSMMappingFile = pathToCountStationToOSMNodesMappingFile;
	}

	//--------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Runs through the data directory and aggregates data that lies in between the specified start and end date.
	 * Then runs through the given network and looks for the links that connect the corresponding fromNode and toNode of each counting station,
	 * specified in a given csv file. Finally, the aggregated data is converted into two matsim-counts-files, one for each combination of columns previouslay defined
	 * which get written out to the specified output directory. With <i>specified</i> i mean given as a parameter to the constructor<br>
	 * tschlenther jul'17
	 */
	public void run(){
		
		init();
		readData();
		  
		writeOutListOfAnalyzedCountStations();
		if(network!=null){
			log.info("Number of nodes in the link: "+network.getNodes().size());
			log.info("Number of links in the link: "+network.getLinks().size());

			readNodeIDsOfCountingStationsAndGetLinkIDs();
		}

		String description = "--Nemo long period count data-- start date: " + this.firstDayOfAnalysis.toString() + " end date:" + this.lastDayOfAnalysis.toString();
		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		String now = format.format(Calendar.getInstance().getTime());
	  	description += "\n created: " + now;
	  	convert(description);
	  
	  	finish();
	}

	protected void readData() {
		File rootDirectory = new File(this.pathToCountData);
		if (! rootDirectory.exists()) throw new RuntimeException(this.pathToCountData + " does not exists.");

		 File[] filesInRoot = rootDirectory.listFiles();
		  if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	if(fileInRootDir.isDirectory()){
		    		int currentYear = Integer.parseInt(fileInRootDir.getName().substring(fileInRootDir.getName().length() - 4));
		    		if(checkIfYearIsToBeAnalyzed(currentYear)){
		    			analyzeYearDir(fileInRootDir,currentYear);
		    		}
		    	}
		    }
		  } else {
			  log.severe("the given root directory of input count data could not be accessed... aborting");
			  this.finish();
		  }
	}

	protected void init() {
		for(String combination: this.countsPerColumnCombination.keySet()){
			Map<String,HourlyCountData> dataMap = new HashMap<String,HourlyCountData>();
			this.countingStationsData.put(combination, dataMap);
			String[] types = combination.split(";");
			for (String s : types){
				if(!this.allNeededColumnHeaders.contains(s)){
					this.allNeededColumnHeaders.add(s);
					if(s.equals(RawDataVehicleTypes.Pkw.toString())) {
						this.allNeededColumnHeaders.add("PLZ");
						this.allNeededColumnHeaders.add("PkwÄ");
					}
				}
				
			}
		}
		
		File outPutDir = new File(this.outputPath.substring(0, this.outputPath.lastIndexOf("/")));
		if (!outPutDir.exists()){
			outPutDir.mkdirs();
		}
		initializeLogger();
	}
	
	protected void convert(String countsDescription) {
		
		for(String combination : this.countsPerColumnCombination.keySet()){
			Counts container = this.countsPerColumnCombination.get(combination) ;
			if(container == null){
				throw new RuntimeException("Counts container should not be empty at this point.");
			} else{
				container.setDescription(container.getDescription() + countsDescription);
			}
			container.setYear(this.lastDayOfAnalysis.getYear());
			
			log.info("start conversion of data for " + combination + "...");
			convertDataToMatSimCounts(container, this.countingStationsData.get(combination));
			this.countsPerColumnCombination.put(combination, container);
		}
	}

	private void convertDataToMatSimCounts(Counts<Link> container, Map<String,HourlyCountData> dataMap){
		int cnt = 0;
		for(String countNrString : dataMap.keySet()){
			String stationID = "NW_" + countNrString;
			
			if(this.notMapMatchedStations.contains(stationID + "_R1") || this.notMapMatchedStations.contains(stationID + "_R2")){
				continue;
			}
			
			cnt ++;
			if(cnt % 50 == 0){
				log.info("converting station nr " + cnt);
			}
			HourlyCountData data = dataMap.get(countNrString);
			
			Id<Link> linkIDDirectionOne = this.linkIDsOfCountingStations.get(stationID + "_R1");
			Id<Link> linkIDDirectionTwo = this.linkIDsOfCountingStations.get(stationID + "_R2");

			if(container == null){
				throw new RuntimeException("Counts container should not be empty at this point.");
			}

			if(data == null){
				log.severe("can not access the basthourlycountdata... the countNrString was " + countNrString );
			}

			if(linkIDDirectionOne == null && !this.notLocatedCountingStations.contains(stationID + "_R1")){
				String problem = "direction 1 of the counting station " + stationID + " was not localised in the given csv file";
				log.severe(problem);
				this.problemsPerCountingStation.put(stationID + "_R1", problem);
//				linkIDDirectionOne = Id.createLinkId("directionNotSpecifiedInCSV_" + stationID + "_R1");
				this.notLocatedCountingStations.add(stationID + "_R1");
			}
			if(linkIDDirectionTwo == null && !this.notLocatedCountingStations.contains(stationID + "_R2")){
				String problem = "direction 2 of the counting station " + stationID + " was not localised in the given csv file";
				log.severe(problem);
				this.problemsPerCountingStation.put(stationID + "_R2", problem);
//				linkIDDirectionTwo = Id.createLinkId("directionNotSpecifiedInCSV_" + stationID + "_R2");
				this.notLocatedCountingStations.add(stationID + "_R2");
			}
				
				Count<Link> countDirOne = null;
				Count<Link> countDirTwo = null;
			try{
				if(linkIDDirectionOne != null && !(this.notLocatedCountingStations.contains(stationID + "_R1")) ){
					countDirOne = container.createAndAddCount(linkIDDirectionOne, data.getId() + "_R1" );
				}
				if(linkIDDirectionTwo != null && !(this.notLocatedCountingStations.contains(stationID + "_R2")) ){
					countDirTwo = container.createAndAddCount(linkIDDirectionTwo, data.getId() + "_R2" );
				}

				for(int i = 1; i < 25; i++) {
					Double valueDirOne = data.getR1Values().get(i);
					Double valueDirTwo = data.getR2Values().get(i);

					if(valueDirOne == null){
						String problem = "station " + stationID + " has a non-valid entry for hour " + i +" in direction one. Please check this. The value in the count file is set to -1."; 
						log.severe( problem + " Error occured at creation nr " + cnt);
						if(this.problemsPerCountingStation.containsKey(stationID)){
							problem = this.problemsPerCountingStation.get(stationID) + problem;
						}
						this.problemsPerCountingStation.put(stationID, problem);
						valueDirOne = -1.;
					}
					if(valueDirTwo == null){
						String problem = "station " + stationID + " has a non-valid entry for hour " + i +" in direction two. Please check this. The value in the count file is set to -1."; 
						log.severe( problem + " Error occured at creation nr " + cnt);
						if(this.problemsPerCountingStation.containsKey(stationID)){
							problem = this.problemsPerCountingStation.get(stationID) + problem;
						}
						this.problemsPerCountingStation.put(stationID, problem);
						valueDirTwo = -1.0;
					}
						if(countDirOne != null)	countDirOne.createVolume(i, Math.ceil(valueDirOne));
						if(countDirTwo != null) countDirTwo.createVolume(i, Math.ceil(valueDirTwo));
				}
				
			} catch(Exception e){
				String str = "\n current station = " + stationID + ". the other station that is already on the link is station " + container.getCount(linkIDDirectionTwo) + " for R1 or station " + container.getCount(linkIDDirectionOne) + " for R2";
				System.out.println(e.getMessage() + str);
				e.printStackTrace();
				
				this.problemsPerCountingStation.put(stationID, e.getMessage() + str);
			}
			
		}
	}

	protected void readNodeIDsOfCountingStationsAndGetLinkIDs() {		
		log.info("...start reading OSM-nodeID's from " + this.pathToOSMMappingFile);
		
		Map<String,Id<Link>> linkIDsOfCounts = new HashMap<String,Id<Link>>();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(pathToOSMMappingFile);
        
        CountLinkFinder linkFinder = new CountLinkFinder(network);
        
        new TabularFileParser().parse(config, new TabularFileHandler() {
        	private boolean header = true;
        	
			@Override
			public void startRow(String[] row) {
				if(!header){
					Id<Link> countLinkID = null;
					if(row.length >= 5){
						log.severe("station " + row[0] + " is commented like this in the map matching csv file: " + row[4]);
						log.severe("it is assumed not to be mapmatched properly and thus gets ignored while data conversion...");
						notMapMatchedStations.add(row[0]);
					}
					else{
						boolean missingNodes = false;
						Node fromNode = network.getNodes().get(Id.createNodeId(Long.parseLong(row[1])));
						if(fromNode == null){
							String problem = "could not find fromNode " + row[1] + " of station " + row[0];
							log.severe(problem);
							log.severe("this means something went wrong in network creation");
							problemsPerCountingStation.put(row[0], problem);
//							log.severe("setting a non-valid link id: " + "noFromNode"  + row[0]);
							linkIDsOfCounts.put(row[0], Id.createLinkId("noFromNode_" + row[0]));
							header = false;
							notLocatedCountingStations.add(row[0]);
							missingNodes = true;
						}
						Id<Node> toNodeID = Id.createNodeId(Long.parseLong(row[2]));
						Node toNode = network.getNodes().get(toNodeID);
						if(toNode == null){
							String problem = "could not find toNode " + row[2] + " of station " + row[0];
							log.severe(problem);
							log.severe("this means something went wrong in network creation");
							problemsPerCountingStation.put(row[0], problem);
//							log.severe("setting a non-valid link id: " + "noToNode"  + row[0]);
							linkIDsOfCounts.put(row[0], Id.createLinkId("noToNode_" + row[0]));
							header = false;
							notLocatedCountingStations.add(row[0]);
							missingNodes = true;
						}

						if (missingNodes) return;

						for(Link outlink : fromNode.getOutLinks().values()){
							if(outlink.getToNode().getId().equals(toNodeID)){
								countLinkID = outlink.getId();
							}
						}
						if(countLinkID == null){
							String problem;
//						problem = "could not find a link directly leading from node " + fromNode.getId() + " to node " + toNodeID;
////						log.severe(problem);
//						problemsPerCountingStation.put(row[0], problem);
							
							countLinkID = linkFinder.getFirstLinkOnTheWayFromNodeToNode(fromNode, toNode);
							if(countLinkID == null){
								problem = "COULD FIND NO PATH LEADING FROM NODE " + fromNode.getId() + " TO NODE " + toNodeID;
								log.severe(problem);
								countLinkID = Id.createLinkId("pathCouldNotBeCreated_" + row[0]);
								problemsPerCountingStation.put(row[0], problem);
								notLocatedCountingStations.add(row[0]);
							}
						}
					}
					
					linkIDsOfCounts.put(row[0], countLinkID);
				}
				header = false;
			}
		
        });
        log.info("-----------------------------------------------------");
        log.info("read in " + linkIDsOfCounts.size() + " link-id's");
        this.linkIDsOfCountingStations = linkIDsOfCounts;
        
        log.info("number of OSM-Node-ID-mappings that were not directly connected by a link, but a path could be calculated: " + linkFinder.getNrOfFoundPaths());
        
        if(linkFinder.getNrOfFoundPaths() >= 1){
        	log.info("writing out a network file for visualisation that contains all reconstructed paths between the corresponding OSM-fromNodes and OSM-toNodes. The first link of a path has capacity of 0, the second capacity of 1 ....");
    		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HH_mm");
        	linkFinder.writeNetworkThatShowsAllFoundPaths(outputPath + "visNetOfReconstructedPaths_" + format.format(Calendar.getInstance().getTime()) + ".xml");
        }
	}

	protected void analyzeYearDir(File rootDirOfYear, int currentYear) {
		log.info("Start analysis of directory " + rootDirOfYear.getPath());
		
		 File[] filesInRoot = rootDirOfYear.listFiles();
		  if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	if(fileInRootDir.isDirectory() && checkIfMonthIsToBeAnalyzed(fileInRootDir.getName())){
		    		analyzeMonth(fileInRootDir, currentYear);
		    	}
		    }
		  } else {
			  log.severe("something is wrong with the input directory .... please look here: " + rootDirOfYear.getAbsolutePath());
			  this.finish();
		  }
	}

	/**
	 * goes through the input data file that contains traffic data of one (long-period!) counting station for one month and aggregates the data in the previously defined way.
	 * the first three rows of the input file define the layout of the file, for more information see the documentation file at
	 * shared-svn\projects\nemo_mercator\40_Data\counts\LandesbetriebStrassenbauNRW_Verkehrszentrale\BASt-Bestandsbandformat_Version2004.pdf
	 * 
	 */
	private void analyzeMonth(File monthDir, int currentYear) {
		log.info("Start to analyze month " + monthDir.getName());
		
		File[] countFiles = monthDir.listFiles();
		  if (countFiles != null) {
			for(int ff = 0; ff<countFiles.length; ff++){
				File countFile = countFiles[ff];
				String name = countFile.getName();
				this.countingStationNames.put(name.substring(0, name.lastIndexOf(".")) , monthDir.getName());
				try {
//					BufferedReader reader = new BufferedReader(new FileReader(countFile));
					//need this for proper encoding
					BufferedReader reader = new BufferedReader( new InputStreamReader(new FileInputStream(countFile), "windows-1256"));
					String headerOne = reader.readLine();
					String headerTwo = reader.readLine();
					String headerThree = reader.readLine();
					
					String countID = headerOne.substring(5, 9);
					
					Long id = Long.parseLong(countID); 
					if(countingStationsToOmit.contains(id)){
						log.info("skipping station " + id);
						continue;
					}
					
					String streetID = headerOne.substring(13,20);
					streetID = streetID.replaceAll("\\s", "");
					String countName = headerOne.substring(21,46);
					
					countName = countName.replaceAll("\\s", "");
					countName = fixEncoding(countID + "_" + countName + "_" + streetID);

					int nrOfLanesDir1 = Integer.parseInt(headerTwo.substring(1, 3));
					int nrOfLanesDir2 = Integer.parseInt(headerTwo.substring(4, 6));
					
					
					int nrOfVehicleTypes = Integer.parseInt(headerThree.substring(4, 6));
					if(nrOfVehicleTypes <9) {
						log.warning("data of count " + countID + "" + countName + " is not differentiating Pkw from at least one other class");
						if(!USE_DATA_WITH_LESS_THAN_9_VEHICLE_CLASSES) {
							log.warning("skipping data set of station " + countID + " because accurancy is not high enough");
						}
					}
					int nrOfVehicleGroups = Integer.parseInt(headerThree.substring(1, 3));	// either 1 => all vehicles in one class or 2 => distinction of heavy traffic
					if(nrOfVehicleGroups == 1) {
						log.info("skipping data set of station " + countID + "" + countName + " because it doesn't differentiate heavy vehicles from normal ones..");
						continue;
					}
					
					Map<String,Integer> baseColumnsOfVehicleTypes = new HashMap<String,Integer>();
					

					//get column number for each header
					String[] headerThreeArray = headerThree.split("\\s+");
					for(int i = 2; i < headerThreeArray.length ; i++){
						String vehicleType = headerThreeArray[i];
						if(this.allNeededColumnHeaders.contains(vehicleType)){
							if( !vehicleType.equals(RawDataVehicleTypes.SV.toString()) ){
								baseColumnsOfVehicleTypes.put(vehicleType, nrOfVehicleGroups*(nrOfLanesDir1 + nrOfLanesDir2) + i -2);
							}else{
								baseColumnsOfVehicleTypes.put(vehicleType, i);
							}
						}
					}
						
					//clarify which column combinations are contained in this file
					List<String> containedCombinationsInThisFile = new ArrayList<String>();
					for(String combination: this.countsPerColumnCombination.keySet()){
						boolean allHeadersInThisCountFile = true;
						String[] headers = combination.split(";");
						for(String header: headers){
							if(!baseColumnsOfVehicleTypes.containsKey(header)){
								allHeadersInThisCountFile = false;
							}
						}
						if(allHeadersInThisCountFile){
							containedCombinationsInThisFile.add(combination);
						}
					}
					
					String line;
					String[] rowData;
					while((line = reader.readLine()) != null) {
						if(line.charAt(6) != 'i'){				//letter i stands for data that was somehow edited after investigation. we'll skip the row
							rowData = line.split("\\s+");
							
							Integer currentMonth = Integer.parseInt(line.substring(2, 4));
							Integer currentDay = Integer.parseInt(line.substring(4, 6));
							
							LocalDate currentDate = LocalDate.of(currentYear, currentMonth, currentDay);
							if (currentDate.isAfter(lastDayOfAnalysis)){
								break;
							}
							if( currentDate.isAfter(firstDayOfAnalysis.minusDays(1))
									&& currentDate.getDayOfWeek().getValue() >= this.weekRange_min && currentDate.getDayOfWeek().getValue() <= this.weekRange_max
									&& !this.datesToIgnore.contains(currentDate) ){
								
								int hour = Integer.parseInt(rowData[1].substring(0, 2));
								
								Map<String,Tuple<Double,Double>> trafficVolumesperVehicleType = new HashMap<String,Tuple<Double,Double>>();
								
								//read traffic volumes for each needed vehicle type (summing up every lane per direction)
								for(String header: baseColumnsOfVehicleTypes.keySet()){
									int jumpLength;
									if(header.equals(header.equals(RawDataVehicleTypes.SV.toString()))){
										jumpLength = nrOfVehicleGroups;
									}
									else{
										jumpLength = nrOfVehicleTypes;
									}
									double vol1 = readTrafficVolume(rowData, baseColumnsOfVehicleTypes.get(header), nrOfLanesDir1, jumpLength);
									double vol2 = readTrafficVolume(rowData, baseColumnsOfVehicleTypes.get(header) + nrOfLanesDir1 * jumpLength, nrOfLanesDir2, jumpLength);
									trafficVolumesperVehicleType.put(header, new Tuple<Double,Double>(vol1,vol2));
								}
								
								//calculate traffic volume for each combination, e.g. combination is. "Pkw+Rad"
								for(String combination : containedCombinationsInThisFile){
									String[] headers = combination.split(";");
									Double sumDir1 = 0.;
									Double sumDir2 = 0.;
									for(String header : headers){
										sumDir1 += trafficVolumesperVehicleType.get(header).getFirst();
										sumDir2 += trafficVolumesperVehicleType.get(header).getSecond();
									}
									
									//get the HourlyCountData object and set volumes
									HourlyCountData data = this.countingStationsData.get(combination).get(countID);
									if(data == null){
										data = new HourlyCountData(countName, null); //ID = countID_countName_streetID
									}
									
									data.computeAndSetVolume(true, hour, sumDir1);
									data.computeAndSetVolume(false, hour, sumDir2);
									this.countingStationsData.get(combination).put(countID,data);
								}
								
							}
						}
					}
					
					reader.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.severe("could not access " + countFile.getAbsolutePath() + "\n the corresponding data is not taken into account");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	
		    }
		  } else {
			  log.severe("the following directory is empty or cannot be accessed. Thus, it is skipped. Path = " + monthDir.getAbsolutePath());
			  this.finish();
		  }
	}
	
	protected String fixEncoding(String string) {
		string = string.replaceAll("ä", "ae");
		string = string.replaceAll("Ä", "Ae");
		string = string.replaceAll("ü", "ue");
		string = string.replaceAll("Ü", "Ue");
		string = string.replaceAll("ö", "oe");
		string = string.replaceAll("Ö", "Oe");
		string = string.replaceAll("ß", "ss");
		return string;
	}

	private double readTrafficVolume(String[] rowData, int baseColumn, int nrOfLanes, int jumpLength){
		double trafficVolume = 0.;
		for(int lane = 1; lane <= nrOfLanes; lane ++){
			String valueOfLaneString = rowData[baseColumn + (lane-1) * jumpLength];
			Double valueOfLane = 0.;
			try{
				if(valueOfLaneString.endsWith("-")){
					valueOfLane = Double.parseDouble(valueOfLaneString.substring(0, valueOfLaneString.length() - 1)); 
				}
				
			} catch(NumberFormatException nfe){
				log.severe("could'nt read traffic volumes. error message: \n" + nfe.getMessage());
			}
			trafficVolume += valueOfLane;
		}
		return trafficVolume;
	}
	
	private boolean checkIfMonthIsToBeAnalyzed(String name) {
		int month = Integer.parseInt(name.substring(name.length() - 2));
		return (month >= this.monthRange_min && month <= monthRange_max && (month <= lastDayOfAnalysis.getMonthValue()) );
	}

	protected boolean checkIfYearIsToBeAnalyzed(Integer dirYear) {
		int startYear = this.firstDayOfAnalysis.getYear();
		int endYear = this.lastDayOfAnalysis.getYear();
		return (dirYear >= startYear && dirYear <= endYear);
	}

	protected void finish() {
		
			String str = "";
			try{
				for(String combination : this.countsPerColumnCombination.keySet()){
					str += combination + ": " + countsPerColumnCombination.get(combination).getCounts().size() + " counts\n";
				}
				log.info("Number of counts per container: \n" + str);
				
			}catch(Exception e){
				e.printStackTrace();
				log.severe("currently str = " + str);
			}
				
			log.info("number of problems that occured while creating matsim counts: " + this.problemsPerCountingStation.size());

//			String allProblems = "";
//			List<String> keySet = new ArrayList<String>();
//			keySet.addAll(this.problemsPerCountingStation.keySet());
//			if(!keySet.isEmpty()){
//				Collections.sort(keySet);
//				for(String station : keySet){
//					allProblems += "\n" + station + ": \t" + this.problemsPerCountingStation.get(station);
//				}
//				log.info("list of these problems per station: \n" + allProblems);
//				log.info("-----------------");
//			}
			
			//checking quality of countData
			
			
			for(String combination : this.countsPerColumnCombination.keySet()){
				log.info("checking data quality of counts for " + combination);
				
				java.util.Iterator it = this.countsPerColumnCombination.get(combination).getCounts().values().iterator();
				List<String> uselessCounts = new ArrayList<String>();
				int nrOfCountsWithPartiallyMissingInformation = 0;
				
				
				while(it.hasNext()){
					Count count = (Count) it.next();
					if (count.getMaxVolume() == null || count.getMaxVolume().getValue() <= 0){
						uselessCounts.add(count.getCsLabel().toString());
						it.remove();
					}
					for(int i = 1; i < 25; i++){
						if(count.getVolume(i).getValue() < 0){
							nrOfCountsWithPartiallyMissingInformation ++;
							break;
						}
					}
				}
				
				log.info("nrOfCounts =" + this.countsPerColumnCombination.get(combination).getCounts().size() + "\n"
						+ " nr of kfz counts that have at least one missing volume = "+ nrOfCountsWithPartiallyMissingInformation
						+ "\n nr of kfz counts that are totally useless = " + uselessCounts.size());
				if(!uselessCounts.isEmpty()){
					String allUselessCounts = "~";
					for(String c : uselessCounts){
						allUselessCounts += c + "\n~";
					}
					log.info("List of all useless counts follows. \n --- !All of these get removed from resulting counts file! \n" + allUselessCounts);
				}
			}
			
			//write info about not located stations
			if(!this.notLocatedCountingStations.isEmpty()){
				log.info("List of all " + notLocatedCountingStations.size() + " counting stations that could not be localised during conversion process.\n these stations were not converted into matsim counts \n");
				String list = "~ ";
				for(String station : notLocatedCountingStations){
					list += station + "\n~ "; 
				}
				log.info(list);
			}
			
			log.info("...closing " + this.getClass().getName() + "...");
	}

	
	private void writeOutListOfAnalyzedCountStations() {
		try {
			  log.info("writing out all counting station names to " + outputPath);
			  File f = new File(outputPath + "allCountingStations_from_" + this.firstDayOfAnalysis.toString() + "_to" + this.lastDayOfAnalysis.toString() + ".txt");
			  if(f.exists()) f.delete();
			  f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter( f ) );
			List<String> allNames = new ArrayList<String>(); 
			allNames.addAll(this.countingStationNames.keySet());
			Collections.sort(allNames);
			
			for(String countName : allNames){
				writer.write(countName + "\t letzter Monat : " + countingStationNames.get(countName) + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	protected void initializeLogger(){
		 SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		 FileHandler fh = null;
		 ConsoleHandler ch = null;
	        try {
	        	fh = new FileHandler(outputPath + "Log_"
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

	//Since, there are two different counts creater, thus, use them to create and write counts file outside. Amit May'18
//	public void writeOutput(String countFilesName){
//
//		String out = "NemoCounts_data_";
//		for(String combination : this.countsPerColumnCombination.keySet()){
//			CountsWriter writer = new CountsWriter(countsPerColumnCombination.get(combination));
//			log.info("writing " + combination + " counts to " + this.outputPath + out + countFilesName + "_" + combination + ".xml");
//			writer.write(this.outputPath + out + countFilesName + "_" + combination + ".xml");
//			log.info("finished writing " + combination + " data");
//		}
//		log.info("....finished writing all output data...");
//	}
	
//	public static void main(String[] args){
//		final String INPUT_COUNT_NODES_MAPPING_CSV= "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/OSMNodeIDs_Testlauf.csv";
//		final String INPUT_COUNT_DATA_ROOT_DIR = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/LandesbetriebStrassenbauNRW_Verkehrszentrale";
//		final String OUTPUT_COUNTS_DIR = "C:/Users/Work/VSP/Nemo/";
//		
//		String[] columns = new String[2];
//		columns[0] = "KFZ";
//		columns[1] = "SV";
//		
//		NemoCountsCreator countCreator = new NemoCountsCreator(columns, null, INPUT_COUNT_DATA_ROOT_DIR, INPUT_COUNT_NODES_MAPPING_CSV, OUTPUT_COUNTS_DIR);
//		
//		countCreator.setFirstDayOfAnalysis(LocalDate.of(2014, 1, 1));
//		countCreator.setLastDayOfAnalysis(LocalDate.of(2014, 1, 2));
//		countCreator.run();
//	}
	
//-----------------------------------------------------------------  SETTERS  ----------------------------------------------------------------------------------------
//	public void setOutputPath (String newOutputPath){
//		this.outputPath = newOutputPath;
//	}
	
	public void setFirstDayOfAnalysis(LocalDate day){
		this.firstDayOfAnalysis = day;
	}
	
	public void setLastDayOfAnalysis(LocalDate day){
		this.lastDayOfAnalysis = day;
	}

	public void setDatesToIgnore(List<LocalDate> datesToIgnore) {
		this.datesToIgnore.clear();
		this.datesToIgnore.addAll(datesToIgnore);
	}

	public void addToStationsToOmit(Long stationID){
		this.countingStationsToOmit.add(stationID);
	}
	
	public void setMonthRangeMin(int monthRange_min) {
		this.monthRange_min = monthRange_min;
	}

	public void setMonthRangeMax(int monthRange_max) {
		this.monthRange_max = monthRange_max;
	}

	public void setWeekRangeMin(int weekRange_min) {
		this.weekRange_min = weekRange_min;
	}

	public void setWeekRangeMax(int weekRange_max) {
		this.weekRange_max = weekRange_max;
	}

	public void setCountingStationNames(Map<String, String> countingStationNames) {
		this.countingStationNames = countingStationNames;
	}

	public void setCountingStationsToOmit(List<Long> countingStationsToOmit) {
		this.countingStationsToOmit.clear();
		this.countingStationsToOmit.addAll(countingStationsToOmit);
	}
	
//-----------------------------------------------------------------  GETTERS  ----------------------------------------------------------------------------------------

	public Counts getCountsOfColumnCombination(String columnHeaderCombination){
		return this.countsPerColumnCombination.get(columnHeaderCombination);
	}

}