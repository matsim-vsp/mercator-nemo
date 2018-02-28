/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.scenarioCreation.cemdap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.matsim.core.utils.gis.ShapeFileReader;
import playground.vsp.demandde.cemdap.input.DemandGeneratorCensus;
import playground.vsp.demandde.cemdap.input.ZoneAndLOSGeneratorV2;

/**
 * Created by amit on 22.06.17.
 */

public class CemdapInputGenerator {

    private static final String municipalityFeatureKey = "KN";
    private static final String plzFeatureKey = "plz";

    public static void main(String[] args) {
        String baseDir;

        if (args.length > 0) {
            baseDir = args[0];
        } else {
            baseDir = "data/cemdap_input/";
        }

        String commuterFileOutgoing1 = baseDir + "/pendlerstatistik/modified/051NRW2009Ga.txt";
        String commuterFileOutgoing2 = baseDir + "/pendlerstatistik/modified/053NRW2009Ga.txt";
        String commuterFileOutgoing3 = baseDir + "/pendlerstatistik/modified/055NRW2009Ga.txt";
        String commuterFileOutgoing4 = baseDir + "/pendlerstatistik/modified/057NRW2009Ga.txt";
        String commuterFileOutgoing5 = baseDir + "/pendlerstatistik/modified/059NRW2009Ga.txt";

        String censusFile = baseDir + "/zensus_2011/Zensus11_Datensatz_Bevoelkerung_NRW.csv";
        String outputBase = baseDir + "/200_" + new SimpleDateFormat("ddMMMyyyy").format(new Date()) + "/";

        //plz file
        String spatialRefinementShapeFile = baseDir+"/shapeFiles/plzBasedPopulation/plz-gebiete_Ruhrgebiet/plz-gebiete_Ruhrgebiet_withPopulation.shp";
        List<String> idsOfMunicipalitiesConsideredForSpatialRefinement = ShapeFileReader
                .getAllFeatures(spatialRefinementShapeFile)
                .stream()
                .map(feature -> feature.getAttribute(municipalityFeatureKey).toString())
                .collect(Collectors.toList());

        // Parameters
        int numberOfPlansPerPerson = 5;
        double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
        double defaultEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
        boolean writeMatsimPlanFiles = true;
        boolean includeChildren = false;


        String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4, commuterFileOutgoing5};

//        {// person and plans file which contains only attributes; these will be required to generate matsim plans files
            DemandGeneratorCensus demandGeneratorCensus = new DemandGeneratorCensus(commuterFilesOutgoing, censusFile, outputBase, numberOfPlansPerPerson,
                    Arrays.asList("05"), defaultAdultsToEmployeesRatio, defaultEmployeesToCommutersRatio);
            demandGeneratorCensus.setWriteMatsimPlanFiles(writeMatsimPlanFiles);
            demandGeneratorCensus.setIncludeChildren(includeChildren);
            demandGeneratorCensus.setIdsOfMunicipalityForSpatialRefinement(new ArrayList<>(new LinkedHashSet<String>(idsOfMunicipalitiesConsideredForSpatialRefinement)));
            demandGeneratorCensus.setFeatureKeyInShapeFileForRefinement(plzFeatureKey);
            demandGeneratorCensus.setMunicipalityFeatureKeyInShapeFile(municipalityFeatureKey);
            demandGeneratorCensus.setShapeFileForSpatialRefinement(spatialRefinementShapeFile);

            demandGeneratorCensus.generateDemand();
//        }
        { // zones and lor files

            String shapeFileLors = baseDir + "/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
            ZoneAndLOSGeneratorV2 zoneAndLOSGeneratorV2 = new ZoneAndLOSGeneratorV2(commuterFilesOutgoing, shapeFileLors, outputBase+"/zoneAndLOS/", municipalityFeatureKey);
            zoneAndLOSGeneratorV2.setDefaultIntraZoneDistance(1.72); // 1.72 miles --> 2.76 km (default value)
            zoneAndLOSGeneratorV2.setShapeFileForRefinement(spatialRefinementShapeFile, plzFeatureKey);
            zoneAndLOSGeneratorV2.setDefaultIntraZoneDistanceForSpatialRefinement(1.1); // 1.1 miles --> 1.77 km
            // assuming average speed of 24 kph in peak hr
            // (see https://de.statista.com/statistik/daten/studie/37200/umfrage/durchschnittsgeschwindigkeit-in-den-15-groessten-staedten-der-welt-2009/)
            zoneAndLOSGeneratorV2.setDurantionDistanceOffPeakRatio_min_mile(3.2); // 3.2 --> 30kph // 1.6 --> 60kph (default value)
            zoneAndLOSGeneratorV2.setDurantionDistancePeakRatio_min_mile(4.8); // 4.8 --> 20kph // 1.9 --> 50kph (default value)
            zoneAndLOSGeneratorV2.generateSupply();
        }
    }
}
