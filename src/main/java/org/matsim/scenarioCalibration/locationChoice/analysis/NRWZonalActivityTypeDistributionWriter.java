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

import org.matsim.NEMOAreaFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.util.NEMOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by amit on 01.02.18.
 */

public class NRWZonalActivityTypeDistributionWriter {

    public static void main(String[] args) {
        for (int i = 1; i < 8; i++) {
            new NRWZonalActivityTypeDistributionWriter().run("run" + i);
        }
    }

    public void run(String runNr) {
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

        NEMOAreaFilter areaFilter = new NEMOAreaFilter(nameToZonesList,
                NEMOUtils.Ruhr_MUNICIPALITY_SHAPE_FILE,
                NEMOUtils.MUNICIPALITY_SHAPE_FEATURE_KEY);

        String outputFilesDir =  "../../runs-svn/nemo/locationChoice/" + runNr + "/output/";
        Network network = NEMOUtils.loadScenarioFromNetwork(outputFilesDir + "/" + runNr + ".output_network.xml.gz")
                                         .getNetwork();

        EventsManager eventsManager = EventsUtils.createEventsManager();

        PersonHomeLocationHandler homeLocationHandler = new PersonHomeLocationHandler(areaFilter, network);
        eventsManager.addHandler(homeLocationHandler);

        PersonActivityTypeCounter personActivityTypeCounter = new PersonActivityTypeCounter();
        eventsManager.addHandler(personActivityTypeCounter);

        new MatsimEventsReader(eventsManager).readFile(outputFilesDir + "/" + runNr + ".output_events.xml.gz");

        Map<String, List<Id<Person>>> zoneNameToListOfPersons = homeLocationHandler.getZoneNameToListOfPersons();
        Map<Id<Person>, List<String>> personId2ActivityList = personActivityTypeCounter.getPersonIdToListOfActivities();
        //first and last activity are same => remove from the list
        personId2ActivityList.values().forEach(acts -> {
            int size = acts.size();
            if (acts.get(0).equals(acts.get(size - 1))) {
                acts.remove(size - 1);
            }
        });

        Map<String, List<String>> zoneNameToListOfActivityTypes =
                zoneNameToListOfPersons.entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(Map.Entry::getKey,
                                               e -> e.getValue().stream()
                                                     .flatMap(p -> personId2ActivityList.get(p).stream())
                                                     .collect(Collectors.toList())));

        String analysisDir = outputFilesDir + "/analysis/";
        if (!new File(analysisDir).exists()) new File(analysisDir).mkdir();

        zoneNameToListOfActivityTypes.entrySet().forEach(e -> {
            SortedMap<String, Long> act2Counter = new TreeMap<>(e.getValue()
                                                                 .stream()
                                                                 .collect(Collectors.groupingBy(Function.identity(),
                                                                         Collectors.counting())));
            String outFile = analysisDir + "/activityType_" + e.getKey() + ".txt";
            writeResults(act2Counter, outFile);
        });
    }

    private void writeResults(SortedMap<String, Long> act2Counter, String outFile) {
        int totalActs = act2Counter.values().stream().map(e -> e.intValue()).reduce(0, Integer::sum);

        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("actType\tnumberOfTrips\ttripShare\n");
            for (String act : act2Counter.keySet()) {
                writer.write(act + "\t" + act2Counter.get(act) + "\t" + String.valueOf(NEMOUtils.round(act2Counter.get(
                        act) * 100.0 / totalActs, 2)) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private class PersonActivityTypeCounter implements ActivityStartEventHandler {

        private final Map<Id<Person>, List<String>> personIdToListOfActivities = new HashMap<>();

        @Override
        public void handleEvent(ActivityStartEvent event) {
            List<String> acts = this.personIdToListOfActivities.getOrDefault(event.getPersonId(), new ArrayList<>());
            acts.add(event.getActType());
            this.personIdToListOfActivities.put(event.getPersonId(), acts);
        }

        @Override
        public void reset(int iteration) {
            this.personIdToListOfActivities.clear();
        }

        private Map<Id<Person>, List<String>> getPersonIdToListOfActivities() {
            return personIdToListOfActivities;
        }
    }
}
