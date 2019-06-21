/**
 * Date: June, 2019
 * CellRelocator class
 * NEMO, Deurbanization Scenario (3)
 * To run use: RunCellRelocator.java
 * Description: Relocates people into cells read from a CSV file.
 */
package org.matsim.nemo;

import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class CellRelocator {
    private static Logger logger = Logger.getLogger("CellRelocator");
    public List<Coord> cells = new ArrayList<>();
    Path relocationData;
    private Population population;
    private QuadTree quadTree;
    private int limits = 16000;
    private int relocatedEverything = 0;
    private int relocatedOnlyHome = 0;
    //Summation of these two ratios must be less than or equal to 1
    final static double relocateEverythingFraction = 0; //eg. 0.4 means 40 percent of population is moving everything
    final static double relocateOnlyHomeFraction = 1;  //eg. 0.3 means 30 percent of population is moving only home

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
     * Getter of Population
     *
     * @return population variable
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Reassigns all home locations based on desired scenarios
     *
     * @param cells List
     */
    public void reassignHome(List<Coord> cells) {
        int personCounter;
        double personsWithinCellCounter = 0;
        List<Double> percentPopulationMoving;
        List<Person> personsWithinCell = new ArrayList<>();
        Id<Person> tempId = null;
        //For Loop: transverses through one cell row
        for (int i = 0; i < cells.size(); i++) {
            //This collects all plans within a specific cell
            personsWithinCell = findPersonsInRange(cells.get(i));

            //This collects the percentage of population moving from the cell
            percentPopulationMoving = parseProbabilities(i);
            //For Loop: tranverses through the list of percentages of population moving
            for (int j = 0; j < percentPopulationMoving.size(); j++) {
                personCounter = 0;
                //While loop: transverses through the activities within the cell to find the ones needed to move
                while (personsWithinCell.size() != 0.0 &&
                        ((double) personCounter / personsWithinCell.size()) < percentPopulationMoving.get(j)
                        && percentPopulationMoving.get(j) != 0.0) {
                    if (i != j) {
                        //This prevents odd number division ie. array index error is prevented
                        if (personCounter >= personsWithinCell.size()) {
                            break;
                        }
                        /* Moves the homes to their new retrospective locations (relocating everything) according
                        to their corresponding fractions. */
                        if (fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                            changeEverythingInPlan(generateCoordInCell(j), personsWithinCell.get(personCounter).getSelectedPlan());
                        }
                        //(relocate only homes)
                        else if (fractionOfPopulationRelocating(population, relocatedOnlyHome, relocateOnlyHomeFraction) && !fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                            changeOnlyHomeInPlan(generateCoordInCell(j), personsWithinCell.get(personCounter).getSelectedPlan());
                        }
                    }
                    //Counters to keep track of activities
                    personCounter++;
                }
            }
            //Clears both lists for rerun
            percentPopulationMoving.clear();

        }
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
     * Initializes the cells from the CSV file
     */
    private void initializeCells() {
        String x;
        String y;
        Coord coord;
        boolean firstLine = false;

        Iterable<CSVRecord> records;
        try (Reader in = new FileReader("/Users/nanddesai/Documents/mercator-nemo/src/relocationInput.csv")) { //Relocation coord file goes here.
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
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
        Coord oldHome = new Coord();

        QuadTree<Person> quadTree = new QuadTree<>(
                envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()
        );
        /*For Loop: transverses persons to accumulate their plans and to put their activities
          within the Quadtree.*/
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                        oldHome = activity.getCoord();
                        quadTree.put(activity.getCoord().getX(), activity.getCoord().getY(), person);
                    }
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
    private List<Person> findPersonsInRange(Coord coord) {
        List<Person> personsWithinRange = new ArrayList<>();
        //Searches the one km square area for activities
        QuadTree.Rect bounds = new QuadTree.Rect(coord.getX() - limits, coord.getY() - limits, coord.getX() + limits, coord.getY() + limits);
        quadTree.getRectangle(bounds, personsWithinRange);

        return personsWithinRange;
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
        QuadTree.Rect bounds = new QuadTree.Rect(coord.getX() - limits, coord.getY() - limits, coord.getX() + limits, coord.getY() + limits);

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
        double tempVal;
        List<Double> probabilities = new ArrayList<>();
        try {
            //Relocation percentage file goes here
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

        return -Math.pow((0.00001 * weight + 2.15), 9) + Math.pow(0.000001 * weight, -2) + 1000;
    }

    /**
     * @param population given from the relocate class
     * @param people     an integer which counts number of people
     * @return either true or false stating if percentage of
     * population has been relocated
     */
    private Boolean fractionOfPopulationRelocating(Population population, int people, double relocatingFrac) {
        return people < relocatingFrac * population.getPersons().values().size();
    }

    /**
     * All activities of the agent are relocated
     * homes relocated using the CSV files
     * other activities using limited radius method
     * @param homeCoord is passed through
     */
    private void changeEverythingInPlan(Coord homeCoord, Plan plan) {
        Geometry homeArea = null;
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
                    relocatedEverything++;
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
                    relocatedOnlyHome++;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                }
            }
        }
    }
}