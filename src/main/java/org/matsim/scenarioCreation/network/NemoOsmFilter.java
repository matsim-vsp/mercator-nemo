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
package org.matsim.scenarioCreation.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader.OsmFilter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.List;
/**
 * @author tschlenther
 *
 */
public class NemoOsmFilter implements OsmFilter {

	private List<Geometry> geometries;
	
	NemoOsmFilter(String pathToShapeFile){
		readShapeFile(pathToShapeFile);
	}
	
	private void readShapeFile(String shapeFile){
		List<Geometry> geometries = new ArrayList<Geometry>();
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(shapeFile)) {
				geometries.add((Geometry)ft.getDefaultGeometry());
		}
		this.geometries = geometries;
	}
	
	@Override
	public boolean coordInFilter(Coord coord, int hierarchyLevel) {

		if(hierarchyLevel <= 4) return true;
		for(Geometry geo : this.geometries){
			if(geo.contains(MGC.coord2Point(coord)) && hierarchyLevel <= 5) return true;
		}
		
		return false;
	}

}
