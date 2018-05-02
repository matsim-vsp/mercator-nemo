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

package org.matsim.scenarioCalibration.modeChoice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.NEMOUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.scenarioCreation.network.NemoNetworkCreator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Created by amit on 15.02.18.
 */

public class RuhrDetailedNetworkGenerator {

    private static final boolean useMultiModalNetworkCleaner = true;
    private static final boolean readOSMFileAndCreateNetwork = true;

    public static void main(String[] args) {
    	String svnDir = "../../repos/shared-svn/";
        String osmfile = svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
        List<String> inputCountNodeMappingFiles = Arrays.asList(
        		svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv",
        		svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv");
        String epsg = NEMOUtils.NEMO_EPSG;
        String prefix = "detailedRuhr_Network_" + new SimpleDateFormat("ddMMyyyy").format(new Date());
        String outDir = svnDir + "projects/nemo_mercator/data/matsim_input/2018-04-19_RuhrDetailedNet_unCleaned/";
        String shapeFile = svnDir + "projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";
        if ( readOSMFileAndCreateNetwork ){
            NemoNetworkCreator nemoNetworkCreator = new NemoNetworkCreator(osmfile,
                    inputCountNodeMappingFiles,
                    epsg,
                    outDir,
                    prefix);
            nemoNetworkCreator.setShapeFileToFilter(shapeFile);

            nemoNetworkCreator.setOSMFilter(new RuhrOSMFilter(shapeFile));
            nemoNetworkCreator.setIncludeBicyclePaths(true);
            nemoNetworkCreator.createNetwork(false, false);

            if (useMultiModalNetworkCleaner){
                // clean for multi-mode
                MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(nemoNetworkCreator.getNetwork());
                cleaner.run(Collections.singleton(TransportMode.car));
                cleaner.run(Collections.singleton(TransportMode.bike));
                cleaner.removeNodesWithoutLinks();
            }
            nemoNetworkCreator.writeNetwork();
        } else {
            if ( useMultiModalNetworkCleaner ){
                Network network = NEMOUtils
                        .loadScenarioFromNetwork(outDir + "detailedRuhr_Network_17022018filteredcleaned_network.xml.gz")
                        .getNetwork();
                MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
                cleaner.run(Collections.singleton(TransportMode.car)); // will not affect bike links--> biggest cluster for car mode
                cleaner.run(Collections.singleton(TransportMode.bike)); // will not affect car links --> biggest cluster for bike mode
                cleaner.removeNodesWithoutLinks();
                new NetworkWriter(network).write(outDir + "detailedRuhr_Network_17022018filteredcleaned_network_2.xml.gz");
            }
        }
    }

    private static class RuhrOSMFilter implements OsmNetworkReader.OsmFilter {
        private List<Geometry> geometries;

        RuhrOSMFilter(String shapeFile) {
            List<Geometry> geometries = new ArrayList<Geometry>();
            for (SimpleFeature ft : ShapeFileReader.getAllFeatures(shapeFile)) {
                geometries.add((Geometry) ft.getDefaultGeometry());
            }
            this.geometries = geometries;
        }

        @Override
        public boolean coordInFilter(Coord coord, int hierarchyLevel) {
            if (hierarchyLevel <= 4) return true;
            for (Geometry geo : this.geometries) {
                if (geo.contains(MGC.coord2Point(coord))) return true; // everything inside Ruhr.
            }
            return false;
        }
    }
}
