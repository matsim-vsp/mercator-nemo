package org.matsim.nemo;

import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.matsim.api.core.v01.TransportMode.car;


public class CellRelocatorTest {
    private static Logger logger = Logger.getLogger("CellRelocatorTest");
    private static Geometry outerGeometry;
    private Population population;
    private double limits = 5;
    private QuadTree quadTree;


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

    @BeforeClass
    public static void setupClass() {
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        outerGeometry = outer;
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

    private void initializeSpatialIndex(Population population) {
        Coord oldHome = new Coord();

        QuadTree<Person> quadTree = new QuadTree<>(
                -20, -20, 20, 20
        );
        /*
        //For Loop: transverses persons to accumulate their plans and to put their activities within the Quadtree.
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if(activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                        oldHome = activity.getCoord();
                        quadTree.put(activity.getCoord().getX(), activity.getCoord().getY(), person);
                    }
                }
            }
        }
         */
        this.quadTree = quadTree;
    }

    /**
     * @param population from test
     * @param home       from test
     * @param work       from test
     * @param counter    from test
     * @return returns a person with a plan
     */
    private Person createPerson(Population population, Coord home, Coord work, int counter) {
        Person person = population.getFactory().createPerson(Id.createPersonId(counter));
        Plan plan = population.getFactory().createPlan();

        Activity homeActivityInTheMorning = population.getFactory().createActivityFromCoord("home_" + counter, home);
        homeActivityInTheMorning.setStartTime(9 * 60 * 60);
        plan.addActivity(homeActivityInTheMorning);

        Leg toWork = population.getFactory().createLeg(car);
        plan.addLeg(toWork);

        Activity workActivity = population.getFactory().createActivityFromCoord("work_" + counter, work);
        workActivity.setEndTime(17 * 60 * 60);
        plan.addActivity(workActivity);

        Leg toHome = population.getFactory().createLeg(car);
        plan.addLeg(toHome);

        Activity homeActivityInTheEvening = population.getFactory().createActivityFromCoord("home_" + counter, home);
        homeActivityInTheEvening.setStartTime(17 * 60 * 60);
        plan.addActivity(homeActivityInTheEvening);

        person.addPlan(plan);

        return person;
    }

    /**
     * Generates a random coordinate within a specified cell area.
     *
     * @param cellId Cell id
     * @return new coordinate randomly generated
     */
    private Coord generateCoordInCell(int cellId, List<Coord> cells) {
        Coord coord = cells.get(cellId);
        //Creates one km squares area to generate random coordinates
        QuadTree.Rect bounds = new QuadTree.Rect(coord.getX() - limits, coord.getY() - limits,
                coord.getX() + limits, coord.getY() + limits);

        double coordX = bounds.minX + (Math.random() * (bounds.maxX - bounds.minX));
        double coordY = bounds.minY + (Math.random() * (bounds.maxY - bounds.minY));

        return new Coord(coordX, coordY);
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

        return -Math.pow((0.00001 * weight + 2.15), 9) + Math.pow(0.000001 * weight, -2) + 1000;
    }

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
            coordX = minX + (Math.random() * (maxX - minX));
            coordY = minY + (Math.random() * (maxY - minY));
            coord = new Coord(coordX, coordY);
        }
        return coord;
    }

    /*
    Relocator helper method will relocate population to another cell used with deportToOneCell test method
     */
    private int helpRelocateToOneCell(List<Coord> cells) {
        Geometry homeArea = null;
        int relocatedEverything = 0;
        int personsChanged = 0;
        int personsWithinCellCounter = 0;
        List<Person> personsWithinRange = new ArrayList<>();
        //Searches the one km square area for activities
        QuadTree.Rect bounds = new QuadTree.Rect(cells.get(0).getX() - limits, cells.get(0).getY() - limits, cells.get(0).getX() + limits, cells.get(0).getY() + limits);
        quadTree.getRectangle(bounds, personsWithinRange);

        while (personsWithinRange.size() != 0.0 && (double) relocatedEverything / population.getPersons().size() < 0.3 && personsWithinCellCounter < personsWithinRange.size()) {
            //This prevents odd number division ie. array index error is prevented
            if (personsChanged >= personsWithinRange.size()) {
                break;
            }

            Coord oldHome = null;
            Activity home = null;
            Coord homeCoord = generateCoordInCell(1, cells);

            for (PlanElement planElement : personsWithinRange.get(personsWithinCellCounter).getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                        oldHome = activity.getCoord();
                        activity.setCoord(homeCoord);
                        homeArea = createCircle(homeCoord, randomRadius());
                        home = activity;
                        relocatedEverything++;
                    } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                        activity.setCoord(home.getCoord());
                    } else if (!activity.getType().contains("home")) {
                        activity.setCoord(coordLimiter(homeArea));
                    }
                }

            }
            personsWithinCellCounter++;
            personsChanged++;
        }
        //Clears both lists for rerun
        personsWithinRange.clear();
        System.out.println("Population size: " + population.getPersons().size() + ", Everything: " + relocatedEverything + ", personsChanged: " + personsChanged + "\n");

        return relocatedEverything;
    }

    @Test
    public void deportToOneCell() throws URISyntaxException {
        Coord home;
        Coord work;
        List<Coord> cells = new ArrayList<>();
        double hx;
        double hy;
        double wx;
        double wy;
        int numOfPeople = 100;

        URL res = getClass().getClassLoader().getResource("relocationCoordinatesTest.csv");
        Path relocationData = Paths.get(res.toURI());

        String x;
        String y;
        Coord coord;
        boolean firstLine = false;

        Iterable<CSVRecord> records;
        try (Reader in = new FileReader(relocationData.toFile())) {
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

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        this.population = population;
        initializeSpatialIndex(population);
        for (int i = 1; i <= numOfPeople; i++) {
            hx = (Math.random() * ((14 - 5) + 1)) + 5;
            hy = (Math.random() * ((4 + 4) + 1)) - 4;
            wx = (Math.random() * ((14 - 5) + 1)) + 5;
            wy = (Math.random() * ((4 + 4) + 1)) - 4;
            home = new Coord(hx, hy);
            work = new Coord(wx, wy);

            Person person = createPerson(population, home, work, i);
            population.addPerson(person);
            quadTree.put(hx, hy, person);
        }

        double relocatedCounter = helpRelocateToOneCell(cells);

        assertTrue("The relocated number of people is incorrect", 0.3 == relocatedCounter / population.getPersons().size());
    }

    /**
     * Finds activities within km square area
     *
     * @param coord Coordinate
     *              Quadtree
     * @return List of activities within the 1km^2 block
     */
    private List<Person> findPersonsInRange(Coord coord) {
        List<Person> personsWithinRange = new ArrayList<>();
        //Searches the one km square area for activities
        QuadTree.Rect bounds = new QuadTree.Rect(coord.getX() - limits, coord.getY() - limits, coord.getX() + limits, coord.getY() + limits);
        quadTree.getRectangle(bounds, personsWithinRange);

        return personsWithinRange;
    }

    /**
     * Parse one line based of the cell number to get probabilities
     * of how the population moves away from this cell.
     * Source:
     * https://www.geeksforgeeks.org/reading-csv-file-java-using-opencv/
     * Accessed on: 20.05.19
     *
     * @param cellNum Cell id
     * @return list of probabilities
     */
    private List<Double> parseProbabilities(int cellNum) {
        int counter = -1;
        double tempVal;
        List<Double> probabilities = new ArrayList<>();
        try {
            URL res = getClass().getClassLoader().getResource("percentageMoving.csv");
            Path relocationData = Paths.get(res.toURI());

            FileReader filereader = new FileReader(relocationData.toFile());

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
     * Helper class for relocation to multiple cells
     *
     * @param cells
     * @return array of integers with populations
     */
    private ArrayList<Integer> helpRelocateMultiple(List<Coord> cells) {
        Geometry homeArea = null;
        ArrayList<Integer> populationsWithinCells = new ArrayList<>();
        int relocatedToCell;
        int personCounter;
        double personsWithinCellCounter = 0;
        List<Double> percentPopulationMoving;
        List<Person> personsWithinCell = new ArrayList<>();
        //For Loop: transverses through one cell row
        for (int i = 0; i < cells.size(); i++) {
            //This collects all plans within a specific cell
            personsWithinCell = findPersonsInRange(cells.get(i));

            System.out.println("Persons within Cell " + i + ": " + personsWithinCell.size());
            //This collects the percentage of population moving from the cell
            percentPopulationMoving = parseProbabilities(i);
            //For Loop: tranverses through the list of percentages of population moving
            for (int j = 0; j < percentPopulationMoving.size(); j++) {
                //Testing purposes only
                int notRelocated = 0;
                personCounter = 0;
                relocatedToCell = 0;
                //While loop: transverses through the activities within the cell to find the ones needed to move
                while (personsWithinCell.size() != 0.0 &&
                        (double) personCounter / personsWithinCell.size() < percentPopulationMoving.get(j)
                        && percentPopulationMoving.get(j) != 0.0) {
                    if (i != j) {
                        Coord homeCoord = generateCoordInCell(j, cells);
                        //This prevents odd number division ie. array index error is prevented
                        if (personCounter >= personsWithinCell.size()) {
                            break;
                        }
                        //Moves the homes to their new retrospective locations (relocating everything)
                        Coord oldHome = null;
                        Activity home = null;

                        for (PlanElement planElement : personsWithinCell.get(personCounter).getSelectedPlan().getPlanElements()) {
                            if (planElement instanceof Activity) {
                                Activity activity = (Activity) planElement;
                                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                                    oldHome = activity.getCoord();
                                    activity.setCoord(homeCoord);
                                    homeArea = createCircle(homeCoord, randomRadius());
                                    home = activity;
                                    relocatedToCell++;
                                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                                    activity.setCoord(home.getCoord());
                                } else if (!activity.getType().contains("home")) {
                                    activity.setCoord(coordLimiter(homeArea));
                                }
                            }
                        }
                    } else {
                        notRelocated++;
                    }
                    personCounter++;

                }
                if (i == j && percentPopulationMoving.get(j) != 0 && personsWithinCell.size() != 0.0)
                    populationsWithinCells.add(notRelocated);
                else if (i != j && percentPopulationMoving.get(j) != 0 && personsWithinCell.size() != 0.0)
                    populationsWithinCells.add(relocatedToCell);
            }

            //Clears both lists for rerun
            percentPopulationMoving.clear();
            personsWithinCell.clear();
        }
        System.out.println("Population size: " + population.getPersons().size());
        System.out.println(populationsWithinCells);
        return populationsWithinCells;
    }

    /**
     * Tests to see if deporting to multiple cells can occur.
     *
     * @throws URISyntaxException
     */
    @Test
    public void deportToMultipleCells() throws URISyntaxException {
        Coord home;
        Coord work;
        List<Coord> cells = new ArrayList<>();
        double hx;
        double hy;
        double wx;
        double wy;
        int numOfPeople = 40;

        URL res = getClass().getClassLoader().getResource("coordForMultipleCellRelocationTest.csv");
        Path relocationData = Paths.get(res.toURI());

        String x;
        String y;
        Coord coord;
        boolean firstLine = false;

        Iterable<CSVRecord> records;
        try (Reader in = new FileReader(relocationData.toFile())) {
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

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        this.population = population;
        initializeSpatialIndex(population);
        for (int i = 1; i <= numOfPeople; i++) {
            hx = (Math.random() * ((14 - 5) + 1)) + 5;
            hy = (Math.random() * ((4 + 4) + 1)) - 4;
            wx = (Math.random() * ((14 - 5) + 1)) + 5;
            wy = (Math.random() * ((4 + 4) + 1)) - 4;
            home = new Coord(hx, hy);
            work = new Coord(wx, wy);

            Person person = createPerson(population, home, work, i);
            population.addPerson(person);
            quadTree.put(hx, hy, person);
        }

        ArrayList<Integer> relocatedPeopleInCells = helpRelocateMultiple(cells);

        assertEquals("Incorrect number of people moved to each cell.", 0.31, (double) relocatedPeopleInCells.get(2) / population.getPersons().values().size(), 0.05);
    }
}
