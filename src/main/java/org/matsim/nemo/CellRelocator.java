package org.matsim.nemo;

import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
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
import java.util.Random;
import java.util.logging.Logger;

public class CellRelocator {
    private static Logger logger = Logger.getLogger("CellRelocator");
    public List<Coord> cells = new ArrayList<>();
    private Path relocationData;
    private Population population;
    private QuadTree quadTree;
    private QuadTree.Rect bounds;
    //Summation of these two ratios must be less than or equal to 1
    final static double relocateEverythingFraction = 1; //eg. 0.4 means 40 percent of population is moving everything
    final static double relocateOnlyHomeFraction = 0;  //eg. 0.3 means 30 percent of population is moving only home
    private Geometry homeArea;

    /**
     * Constructor
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
     * Creates a circle shape using geotools
     * Link: http://docs.geotools.org/stable/userguide/library/jts/geometry.html
     *
     * @param coord  coordinates of home are passed in
     * @param RADIUS radius value is passed in meters
     * @return Circle shape as a geometry
     */
    private static Geometry createCircle(Coord coord, final double RADIUS) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(coord.getX(), coord.getY()));
        shapeFactory.setSize(RADIUS * 2);
        return shapeFactory.createCircle();
    }

    /**
     * Main method
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
        cellRelocator.reassignHome(cells);

        logger.info("");
        PopulationWriter writer = new PopulationWriter(cellRelocator.getPopulation()); //Writes population
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated_to_cells.xml.gz");
    }

    /**
     * Getter of Population
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

        QuadTree<Plan> quadTree = new QuadTree<>(
                envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()
        );
        //For Loop: transverses persons to accumulate their plans and to put their activities
        //within the Quadtree.
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    quadTree.put(activity.getCoord().getX(), activity.getCoord().getY(), person.getSelectedPlan());
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
    private List<Plan> findPlansInRange(Coord coord) {
        List<Plan> planWithinRange = new ArrayList<>();
        //Searches the one km square area for activities
        bounds = new QuadTree.Rect(coord.getX() - 500, coord.getY() - 500, coord.getX() + 500, coord.getY() + 500);
        quadTree.getRectangle(bounds, planWithinRange);
        return planWithinRange;
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
     * Generates coordinnates limited within the home area geometry
     *
     * @return coord which are within range
     */
    private Coord coordLimiter(Geometry homeArea) {
        //Max and Min values
        Envelope envelope = homeArea.getEnvelopeInternal();

        final double maxX = envelope.getMaxX();
        final double minX = envelope.getMinX();
        final double maxY = envelope.getMaxY();
        final double minY = envelope.getMinY();

        double coordX = 0;
        double coordY = 0;
        Coord coord = new Coord(coordX, coordY);

        while (!homeArea.contains(MGC.coord2Point(coord))) {
            coordX = minX + (Math.random() * (maxX - minX)); //Random double value between min longitude value and max latitude value
            coordY = minY + (Math.random() * (maxY - minY)); //Random double value between min latitude value and max latitude value
            coord = new Coord(coordX, coordY);
        }
        return coord;
    }

    /**
     * This generates a random radius value to be used as the homeArea radii
     * Refer to : https://www.desmos.com/calculator/jbgvmtejrd
     * Eqn: y = -(0.00001x+2.15)^9+(0.000001x)^-2+1000
     *
     * @return randomRadius
     */
    private double randomRadius() {
        final int maxWeightParam = 24816; //x-values in the graph/equation
        final int minWeightParam = 3160; //eg. 3160 value here means approx. 100000m minimum radius

        Random ran = new Random();
        double weight = ran.nextInt(((maxWeightParam - minWeightParam) + 1)) + minWeightParam;

        double randomRadius = -Math.pow((0.00001 * weight + 2.15), 9) + Math.pow(0.000001 * weight, -2) + 1000;

        //Returns the double random radius which is generated from graph.
        return randomRadius;
    }

    /**
     * @param population given from the relocate class
     * @param people     an integer which counts number of people
     * @return either true or false stating if percentage of
     * <p>
     * population has been relocated
     */
    public Boolean fractionOfPopulationRelocating(Population population, int people, double relocatingFrac) {
        final double fracRelocating = relocatingFrac; //given as a decimal. eg. 0.50 is 50%
        return people <= fracRelocating * population.getPersons().size();
    }

    /**
     * Converts the ID at the end of the activity type into an integer.
     *
     * @param act string that says what activity type it is
     * @return
     */
    private int getId(String act) {
        String id = act.replaceAll("[^\\d]", "");
        int idNum = Integer.parseInt(id);
        return idNum;
    }

    /**
     * All activities of the agent are relocated
     * homes relocated using the CSV files
     * other activities using limited radius method
     * @param homeCoord is passed through
     */
    private void changeEverythingInPlan(Coord homeCoord, Plan plan) {
        Coord oldHome = null;
        Activity home = null;

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(homeCoord);
                    homeArea = createCircle(homeCoord, randomRadius());
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                } else if (!activity.getType().contains("home")) {
                    activity.setCoord(coordLimiter(homeArea));
                }
            }
        }
    }

    /**
     * This changes the plan if only the home is needed to be moved.
     * Other activities kept the same
     */
    private void changeOnlyHomeInPlan(Coord homeCoord, Plan plan) {
        Coord oldHome = null;
        Activity home = null;

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(homeCoord);
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                }
            }
        }
    }

    /**
     * Reassigns all home locations based on desired scenarios
     *
     * @param cells
     */
    public void reassignHome(List<Coord> cells) {
        int relocatedEverything = 1;
        int relocatedHomeOnly = 1;
        List<Double> percentPopulationMoving = new ArrayList<>();
        List<Plan> plansWithinCell = new ArrayList<>();
        //For Loop: transverses through one cell row
        for (int i = 0; i < cells.size(); i++) {
            int plansChanged = 0;
            //This collects all activities within a specific cell
            plansWithinCell = findPlansInRange(cells.get(i));
            //This collects the percentage of population moving from the cell
            percentPopulationMoving = parseProbabilities(i);
            //For Loop: tranverses through the list of percentages of population moving
            for (int j = 0; j < percentPopulationMoving.size(); j++) {
                int planCounter = 0;
                //While loop: transverses through the activities within the cell to find the ones needed to move
                while (plansWithinCell.size() != 0.0 &&
                        ((double) planCounter / plansWithinCell.size()) < percentPopulationMoving.get(j)
                        && i != j && percentPopulationMoving.get(j) != 0.0) {
                    //This prevents odd number division ie. array index error is prevented
                    if (plansChanged >= plansWithinCell.size()) {
                        break;
                    }
                    //Moves the homes to their new retrospective locations (relocating everything)
                    if (fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                        changeEverythingInPlan(generateCoordInCell(j), plansWithinCell.get(plansChanged));
                        relocatedEverything++;
                    }
                    //(relocate only homes)
                    else if (fractionOfPopulationRelocating(population, relocatedHomeOnly, relocateOnlyHomeFraction) && !fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                        changeOnlyHomeInPlan(generateCoordInCell(j), plansWithinCell.get(plansChanged));
                        relocatedHomeOnly++;
                    }
                    //Counters to keep track of activities
                    planCounter++;
                    plansChanged++;
                }
            }
            //Clears both lists for rerun
            percentPopulationMoving.clear();
            plansWithinCell.clear();
        }
    }
}