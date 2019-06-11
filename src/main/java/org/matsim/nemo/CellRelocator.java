package org.matsim.nemo;

import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CellRelocator {
    private static Logger logger = Logger.getLogger("CellRelocator");
    public List<Coord> cells = new ArrayList<>();
    private Path relocationData;
    private Population population;
    private QuadTree quadTree;
    private QuadTree.Rect bounds;

    /**
     * Constructor
     *
     * @param relocationData Path to relocation file
     * @param population     Population population
     * @param outerGeometry  Outer geometry
     */
    CellRelocator(Path relocationData, Population population, Geometry outerGeometry) {
        initializeCells();
        initializeSpatialIndex(population, outerGeometry);
        this.relocationData = relocationData;
        this.population = population;
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

    /**
     * Main method
     *
     * @param args args
     */
    public void main(String[] args) {
        String inputFile = "/Users/nanddesai/Documents/NEMOProject/outputPath/nemo_baseCase_089.output_plans_reducedpopulation.xml.gz";
        Path relocationData = Paths.get("/Users/nanddesai/Documents/mercator-nemo/src/relocationInput.csv");
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        PopulationReader reader = new PopulationReader(scenario);

        reader.readFile(inputFile);

        CellRelocator cellRelocator = new CellRelocator(relocationData, scenario.getPopulation(), outer);
        cellRelocator.reassignActivity(cells);

        logger.info("");
        PopulationWriter writer = new PopulationWriter(cellRelocator.getPopulation()); //Writes population
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated_to_cells.xml.gz");
    }

    /**
     * Getter of Population
     *
     * @return population variable
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Initializes the cells from the CSV file
     */
    private void initializeCells() {
        String x;
        String y;
        Coord coord;
        boolean firstLine = false;

        Iterable<CSVRecord> records;
        try (Reader in = new FileReader("/Users/nanddesai/Documents/mercator-nemo/src/relocationInput.csv")) {
            records = CSVFormat.EXCEL.parse(in);
            for (CSVRecord record : records) {
                if (!firstLine) {
                    firstLine = true;
                } else if (record.iterator().hasNext()) {
                    x = record.get(1);
                    y = record.get(2);
                    coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
                    cells.add(coord);
                } else {
                    break;
                }
            }
            logger.info("CSV Coordinates File has been parsed.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the quadtree
     *
     * @param population    Population
     * @param outerGeometry the geometry of the entire map
     */
    private void initializeSpatialIndex(Population population, Geometry outerGeometry) {
        Envelope envelope = outerGeometry.getEnvelopeInternal();

        QuadTree<Activity> quadTree = new QuadTree<>(
                envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()
        );
        //For Loop: transverses persons to accumulate their plans and to put their activities
        //within the Quadtree.
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    quadTree.put(activity.getCoord().getX(), activity.getCoord().getY(), activity);
                }
            }
        }
        this.quadTree = quadTree;
    }

    /**
     * Finds activities within km square area
     *
     * @param coord Coordinate
     *              Quadtree
     * @return List of activities within the 1km^2 block
     */
    private List<Activity> findActivitiesInRange(Coord coord) {
        List<Activity> activitiesWithinRange = new ArrayList<>();
        //Searches the one km square area for activities
        bounds = new QuadTree.Rect(coord.getX() - 500, coord.getY() - 500, coord.getX() + 500, coord.getY() + 500);
        quadTree.getRectangle(bounds, activitiesWithinRange);
        return activitiesWithinRange;
    }

    /**
     * Generates a random coordinate within a specified cell area.
     *
     * @param cellId Cell id
     * @return new coordinate randomly generated
     */
    private Coord generateCoordInCell(int cellId) {
        Coord coord = cells.get(cellId);
        //Creates one km squares area to generate random coordinates
        bounds = new QuadTree.Rect(coord.getX() - 500, coord.getY() - 500, coord.getX() + 500, coord.getY() + 500);

        double coordX = bounds.minX + (Math.random() * (bounds.maxX - bounds.minX));
        double coordY = bounds.minY + (Math.random() * (bounds.maxY - bounds.minY));

        return new Coord(coordX, coordY);
    }

    /**
     * Parse one line based of the cell number to get probabilities
     * of how the population moves away from this cell.
     * Source:
     * https://www.geeksforgeeks.org/reading-csv-file-java-using-opencv/
     *
     * @param cellNum Cell id
     * @return list of probabilities
     */
    private List<Double> parseProbabilities(int cellNum) {
        int counter = -1;
        double tempVal = 0;
        List<Double> probabilities = new ArrayList<>();
        try {
            FileReader filereader = new FileReader("/Users/nanddesai/Documents/mercator-nemo/src/probabilitiesOfRelocation.csv");

            CSVReader csvReader = new CSVReader(filereader);
            String[] nextRecord;

            while ((nextRecord = csvReader.readNext()) != null) {
                for (String cell : nextRecord) {
                    if (counter == cellNum) {
                        tempVal = Double.parseDouble(cell);
                        probabilities.add(tempVal);
                    }
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        probabilities.remove(0);
        return probabilities;
    }

    /**
     * Reassign activities
     *
     * @param cells The list of cells
     */
    public void reassignActivity(List<Coord> cells) {
        List<Double> percentPopulationMoving = new ArrayList<>();
        List<Activity> activitiesWithinCell = new ArrayList<>();

        //For Loop: transverses through one cell row
        for (int i = 0; i < cells.size(); i++) {
            int activitiesChanged = 0;
            //This collects all activities within a specific cell
            activitiesWithinCell = findActivitiesInRange(cells.get(i));
            //This collects the percentage of population moving from the cell
            percentPopulationMoving = parseProbabilities(i);
            //For Loop: tranverses through the list of percentages of population moving
            for (int j = 0; j < percentPopulationMoving.size(); j++) {
                int actCounter = 0;
                //While loop: transverses through the activities within the cell to find the ones needed to move
                while (activitiesWithinCell.size() != 0.0 &&
                        ((double) actCounter / activitiesWithinCell.size()) < percentPopulationMoving.get(j)
                        && i != j && percentPopulationMoving.get(j) != 0.0) {
                    //This prevents odd number division ie. array index error is prevented
                    if (activitiesChanged >= activitiesWithinCell.size()) {
                        break;
                    }
                    //Moves the activities to their new retrospective locations
                    activitiesWithinCell.get(activitiesChanged).setCoord(generateCoordInCell(j));
                    //Counters to keep track of activities
                    actCounter++;
                    activitiesChanged++;
                }
            }
        }
        //Clears both lists for rerun
        percentPopulationMoving.clear();
        activitiesWithinCell.clear();
    }
}
