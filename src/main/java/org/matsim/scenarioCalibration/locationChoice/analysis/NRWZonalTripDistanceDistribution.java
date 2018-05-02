/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.scenarioCalibration.locationChoice.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.NEMOAreaFilter;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

/**
 * Created by amit on 01.02.18.
 */

public class NRWZonalTripDistanceDistribution {

    public static void main(String[] args) {
        for (int i=1; i<8; i++){
            new NRWZonalTripDistanceDistribution().run("run"+i);
        }
    }

    public void run(String runNr) {
        // from the file : NEMOUtils.Ruhr_MUNICIPALITY_SHAPE_FILE
        Map<String, List<String>> nameToZonesList = new HashMap<String, List<String>>() {{
            put("Essen", Arrays.asList("05113000"));
            put("Bochum", Arrays.asList("05911000"));
            put("EnnepeRuhrKreis", Arrays.asList("05954004",
                    "05954008",
                    "05954012",
                    "05954016",
                    "05954020",
                    "05954024",
                    "05954028",
                    "05954032",
                    "05954036"));
        }};

        Map<String, List<Double>> nameToDistanceClasses = new HashMap<String, List<Double>>(){{
            put("Essen",Arrays.asList(0., 1000., 3000., 5000., 10000.));//i.e. 0+, 1km+, 3km+, 5km+, 10km+ etc
            put("Bochum",Arrays.asList(0., 1000., 3000., 5000., 10000.));
            put("EnnepeRuhrKreis", Arrays.asList(0., 500., 1000., 2000., 5000., 10000., 20000., 50000.));
        }};

        NEMOAreaFilter areaFilter = new NEMOAreaFilter(nameToZonesList,
                NEMOUtils.Ruhr_MUNICIPALITY_SHAPE_FILE,
                NEMOUtils.MUNICIPALITY_SHAPE_FEATURE_KEY);

        String outputFilesDir =  "../../runs-svn/nemo/locationChoice/" + runNr + "/output/";
        Network network = NEMOUtils.loadScenarioFromNetwork(outputFilesDir + "/" + runNr + ".output_network.xml.gz")
                                         .getNetwork();
        EventsManager eventsManager = EventsUtils.createEventsManager();

        PersonHomeLocationHandler homeLocationHandler = new PersonHomeLocationHandler(areaFilter, network);
        eventsManager.addHandler(homeLocationHandler);

//        TripDistanceHandler tripDistanceHandler = new TripDistanceHandler(network);
//        eventsManager.addHandler(tripDistanceHandler);
//
//        new MatsimEventsReader(eventsManager).readFile(outputFilesDir + "/" + runNr + ".output_events.xml.gz");
//
//        Map<String, List<Id<Person>>> zoneNameToListOfPersons = homeLocationHandler.getZoneNameToListOfPersons();
//        Map<Id<Person>, List<Double>> idListMap = tripDistanceHandler.getMode2PersonId2TravelDistances().get(TransportMode.car);
//
//        Map<String, List<Double>> zoneNameToListOfTripDistances =
//                zoneNameToListOfPersons.entrySet()
//                                       .stream()
//                                       .collect(Collectors.toMap(Map.Entry::getKey,
//                                               e -> e.getValue().stream()
//                                                       .flatMap(p -> idListMap.get(p).stream())
//                                                       .collect(Collectors.toList())));
//
//        String analysisDir = outputFilesDir+"/analysis/";
//        if (! new File(analysisDir).exists() ) new File(analysisDir).mkdir();
//
//        BufferedWriter writer = IOUtils.getBufferedWriter(analysisDir+"/avgTripDistances.txt");
//        try {
//            writer.write("Zone\tavgTripDistanceInM\tnumberOfTrips\n");
//            for(String zone : zoneNameToListOfTripDistances.keySet()){
//                writer.write(zone+"\t"+String.valueOf(
//                        ListUtils.doubleMean(zoneNameToListOfTripDistances.get(zone) ) ) + "\t"+ zoneNameToListOfTripDistances.get(zone).size() +"\n" );
//            }
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Data is not written/read. Reason : " + e);
//        }
//
//        for(String zone : zoneNameToListOfTripDistances.keySet()) {
//            SortedMap<Double, Integer> distanceClass2TripCount = getDistanceBinToCounts(nameToDistanceClasses.get(zone), zoneNameToListOfTripDistances.get(zone));
//
//            String outFile = analysisDir+"/tripDistanceDistribution_"+zone+".txt";
//            writeResults(distanceClass2TripCount, outFile);
//        }
    }

    private void writeResults(SortedMap<Double, Integer> distanceClassToTrips, String outFile) {
//        SortedMap<Double, Double> distanceClass2ShareOfTrips = MapUtils.getIntPercentShare(distanceClassToTrips);
//        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
//        try {
//            writer.write("distanceClassInMeters\tnumberOfTrips\ttripShare\n");
//            for (Double distClass : distanceClass2ShareOfTrips.keySet()){
//                writer.write(String.valueOf(distClass+"+")+"\t"+distanceClassToTrips.get(distClass)+"\t"+distanceClass2ShareOfTrips.get(distClass)+"\n");
//            }
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Data is not written/read. Reason : " + e);
//        }
    }

    private SortedMap<Double, Integer> getDistanceBinToCounts(List<Double> distanceClasses, List<Double> distances){
        SortedMap<Double, Integer> distanceClassToNumberOfTrips = new TreeMap<>();
        for (Double dist : distances) {
            for (int i=0; i<distanceClasses.size(); i++){
                if ( i == distanceClasses.size()-1 ) { // last distance bin
                    if (dist <= distanceClasses.get(i)) throw new RuntimeException("dist "+dist+" should not be smaller than "+distanceClasses.get(i)+". Where distance classes are "+distanceClasses);
                    distanceClassToNumberOfTrips.put( distanceClasses.get(i), distanceClassToNumberOfTrips.getOrDefault(distanceClasses.get(i),0) + 1 );
                } else {
                    if (dist >= distanceClasses.get(i) && dist < distanceClasses.get(i+1)){
                        distanceClassToNumberOfTrips.put( distanceClasses.get(i), distanceClassToNumberOfTrips.getOrDefault(distanceClasses.get(i),0) + 1 );
                        break;
                    }
                }
            }
        }
        return distanceClassToNumberOfTrips;
    }
}
