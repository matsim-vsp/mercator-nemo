package org.matsim.nemo;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunCellRelocator {
    /**
     * Main method
     * @param args args
     */
    public static void main(String[] args) {
        String inputFile = "/Users/nanddesai/Documents/NEMOProject/outputPath/nemo_baseCase_089.output_plans_reducedpopulation.xml.gz";
        Path relocationData = Paths.get("/Users/nanddesai/Documents/mercator-nemo/src/relocationInput.csv");
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        PopulationReader reader = new PopulationReader(scenario);

        reader.readFile(inputFile);

        CellRelocator cellRelocator = new CellRelocator(relocationData, scenario.getPopulation(), outer);
        cellRelocator.reassignHome(cellRelocator.cells);

        PopulationWriter writer = new PopulationWriter(cellRelocator.getPopulation()); //Writes population
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated_to_cells.xml.gz");
    }
    /**
     * @param pathToFile the path to the shape file
     * @return Geometry from path file
     */
    private static Geometry getFirstGeometryFromShapeFile(Path pathToFile) {
        for (SimpleFeature feature : ShapeFileReader.getAllFeatures(pathToFile.toString())) {
            return (Geometry) feature.getDefaultGeometry();
        }
        throw new RuntimeException("Runtime exception/error, geometry is broken. Unexpected Error.");
    }

}
