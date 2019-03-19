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

package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.util.NEMOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by amit on 15.02.18.
 */

public class RuhrDetailedNetworkGenerator {

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        final String osmfile = arguments.svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/network/06042018/NRW_completeTransportNet.osm.gz";
        final List<String> inputCountNodeMappingFiles = Arrays.asList(
                arguments.svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/OSMNodeIDs_Dauerzaehlstellen.csv",
                arguments.svnDir + "projects/nemo_mercator/data/matsim_input/zz_archive/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-allStationsInclNotFound.csv");
        final String epsg = NEMOUtils.NEMO_EPSG;
        final String prefix = "detailedRuhr_Network_" + new SimpleDateFormat("ddMMyyyy").format(new Date());
        final String outDir = arguments.svnDir + "projects/nemo_mercator/data/matsim_input/2018-05-2018-05-28_shorterIntraZonalDist/";
        final String shapeFile = arguments.svnDir + "projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";

        Network network = null;

        if (arguments.useExistingNetwork && arguments.useMultiModalNetworkCleaner) {

            network = NEMOUtils
                    .loadScenarioFromNetwork(outDir + "2018-05-03_NRW_coarse_filteredcleaned_network.xml.gz")
                    .getNetwork();
            MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
            cleaner.run(Collections.singleton(TransportMode.car)); // will not affect bike links--> biggest cluster for car mode
            cleaner.run(Collections.singleton(TransportMode.bike)); // will not affect car links --> biggest cluster for bike mode
            cleaner.removeNodesWithoutLinks();
        } else if (!arguments.useExistingNetwork) {
            NemoNetworkCreator nemoNetworkCreator = new NemoNetworkCreator(osmfile,
                    inputCountNodeMappingFiles,
                    epsg,
                    outDir,
                    prefix);
            nemoNetworkCreator.setShapeFileToFilter(shapeFile);

            nemoNetworkCreator.setOSMFilter(new RuhrOSMFilter(shapeFile));
            nemoNetworkCreator.setIncludeBicyclePaths(true);
            nemoNetworkCreator.createNetwork(false, false);
            network = nemoNetworkCreator.getNetwork();

            if (arguments.useMultiModalNetworkCleaner) {
                MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
                cleaner.run(Collections.singleton(TransportMode.car));
                cleaner.run(Collections.singleton(TransportMode.bike));
                cleaner.removeNodesWithoutLinks();
            }

            if (arguments.addRideOnCarLinks) {
                network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains(TransportMode.car)).forEach(l -> {
                    Set<String> modes = new HashSet<>(l.getAllowedModes());
                    modes.add(TransportMode.ride);
                    l.setAllowedModes(modes);
                });
            }
            nemoNetworkCreator.writeNetwork();
        }
        new NetworkWriter(network).write(outDir + "2018-05-03_NRW_coarse_filteredcleaned_network.xml.gz");
    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true)
        private String svnDir;

        @Parameter(names = "-useExistingNetwork", description = "default is falsedil")
        private boolean useExistingNetwork = false;

        @Parameter(names = "-useMultiModalNetworkCleaner", description = "default is true")
        private boolean useMultiModalNetworkCleaner = true;

        @Parameter(names = "-addRideOnCarLinks", description = "default is true")
        private boolean addRideOnCarLinks = true;
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
