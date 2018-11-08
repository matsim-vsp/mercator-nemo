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

package org.matsim.scenarioCalibration.marginals;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.util.NEMOUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Take selected plans and persons who originate, terminate any trip to Ruhr boundary or pass through it.
 * This will also remove routes (see switch) so that a detailed network can be used.
 * <p>
 * Created by amit on 12.02.18.
 */

public class FilterInitialNRWPopulationForRuhr {
    // Some assumptions
    private final boolean keepOnlySelectedPlans;
    private final Population outPopulation;
    private final Collection<String> zoneIds = new ArrayList<>();

    FilterInitialNRWPopulationForRuhr(String inputPopulationFile, String outputPopulationFile, String municipalityShapeFile, String municipalityShapeFeatureKey,
    		String plzShapeFile, String plzShapeFeatureKey, boolean keepOnlySelectedPlans) {
    	
    	Population inputPopulation = NEMOUtils.loadScenarioFromPlans(inputPopulationFile).getPopulation();
    	
        this.zoneIds.addAll(ShapeFileReader.getAllFeatures(municipalityShapeFile)
                                           .stream()
                                           .map(f -> (String) f.getAttribute(municipalityShapeFeatureKey))
                                           .collect(Collectors.toList()));

        this.zoneIds.addAll(ShapeFileReader.getAllFeatures(plzShapeFile)
                                           .stream()
                                           .map(f -> (String) f.getAttribute(plzShapeFeatureKey))
                                           .collect(Collectors.toList()));
        this.outPopulation = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();
        this.keepOnlySelectedPlans = keepOnlySelectedPlans;
        
        inputPopulation.getPersons().values().stream().filter(this::keepPerson).forEach(this::cloneAndAddPerson);
        new PopulationWriter(this.outPopulation).write(outputPopulationFile);
    }

    public static void main(String[] args) {
    	InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);
    	
        new FilterInitialNRWPopulationForRuhr(arguments.inputPopulationFile, arguments.outputPopulationFile, arguments.municipalityShapeFile,
        		arguments.municipalityShapeFeatureKey, arguments.plzShapeFile, arguments.plzShapeFeatureKey, arguments.keepOnlySelectedPlans);
    }

    private boolean keepPerson(Person person) {
        if (keepOnlySelectedPlans){
            return person.getSelectedPlan()
                         .getPlanElements()
                         .stream().filter(Activity.class::isInstance)
                    .map(pe -> pe.getAttributes().getAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey))
                         .anyMatch(this.zoneIds::contains);
        } else {
            return person.getPlans()
                         .stream()
                         .flatMap(plan -> plan.getPlanElements().stream())
                         .filter(Activity.class::isInstance)
                    .map(pe -> pe.getAttributes().getAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey))
                         .anyMatch(this.zoneIds::contains);
        }
    }

    private void cloneAndAddPerson(Person person) { // only selected plan
        Person outPerson = this.outPopulation.getFactory().createPerson(person.getId());
        if (keepOnlySelectedPlans) {
            outPerson.addPlan(person.getSelectedPlan());
        } else {
            outPerson = person;
        }
        this.outPopulation.addPerson(outPerson);
    }
    
    private static class InputArguments {
        @Parameter(names = "-inputPopulationFile", required = true)
        private String inputPopulationFile;
        
        @Parameter(names = "-outputPopulationFile", required = true)
        private String outputPopulationFile;
        
        @Parameter(names = "-municipalityShapeFile", required = true)
        private String municipalityShapeFile;
        
        @Parameter(names = "-municipalityShapeFeatureKey", required = true)
        private String municipalityShapeFeatureKey;
        
        @Parameter(names = "-plzShapeFile", required = true)
        private String plzShapeFile;
        
        @Parameter(names = "-plzShapeFeatureKey", required = true)
        private String plzShapeFeatureKey;
        
        @Parameter(names = "-keepOnlySelectedPlans", required = false)
        private Boolean keepOnlySelectedPlans = false;
    }
}