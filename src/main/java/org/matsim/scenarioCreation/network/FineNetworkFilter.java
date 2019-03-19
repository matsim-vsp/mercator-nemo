package org.matsim.scenarioCreation.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.util.ArrayList;
import java.util.List;

public class FineNetworkFilter implements OsmNetworkReader.OsmFilter {

    private final List<Geometry> geometries = new ArrayList<>();

    FineNetworkFilter(String pathToShapeFile) {
        ShapeFileReader.getAllFeatures(pathToShapeFile).forEach(feature -> geometries.add((Geometry) feature.getDefaultGeometry()));
    }

    @Override
    public boolean coordInFilter(Coord coord, int hierarchyLevel) {
        // use all streets which are level 4 or less (motorways and secondary streets)
        if (hierarchyLevel <= 4) return true;

        // if coord is within the supplied shape use every street regardless of level
        return geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(coord)));
    }
}
