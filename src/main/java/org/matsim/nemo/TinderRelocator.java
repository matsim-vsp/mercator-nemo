/**
 * TinderRelocator.java
 * NEMO Scenario 3
 */
package org.matsim.nemo;

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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Logger;


public class TinderRelocator {
    private Geometry innerGeometry;
    private Geometry outerGeometry;
    private Geometry homeArea;
    private Population population;
    //Summation of these two ratios must be less than or equal to 1
    final static double relocateEverythingFraction = 1; //eg. 0.4 means 40 percent of population is moving everything
    final static double relocateOnlyHomeFraction = 0;  //eg. 0.3 means 30 percent of population is moving only home
    private static Logger logger = Logger.getLogger("TinderRelocator");

    /**
     * @param population the population
     * @param innerGeometry the interior
     * @param outerGeometry the exterior
     */
    TinderRelocator(Population population, Geometry innerGeometry, Geometry outerGeometry) {
        this.population = population;
        this.innerGeometry = innerGeometry;
        this.outerGeometry = outerGeometry;

    }

    /**
     * @param coord coordinates of home are passed in
     * @param RADIUS radius value is passed in meters
     * @return circle shape as a geometry
     */
    //http://docs.geotools.org/stable/userguide/library/jts/geometry.html
    private static Geometry createCircle(Coord coord, final double RADIUS) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(coord.getX(), coord.getY()));
        shapeFactory.setSize(RADIUS * 2);
        return shapeFactory.createCircle();
    }

    /**
     * @param args nothing is passed through
     */
    public static void main(String[] args) {

        String inputFile = "/Users/nanddesai/Documents/NEMOProject/outputPath/nemo_baseCase_089.output_plans_reducedpopulation.xml.gz";
        Path shapeRuhrgebiet = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp");
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry inner = getFirstGeometryFromShapeFile(shapeRuhrgebiet);
        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        PopulationReader reader = new PopulationReader(scenario);//Reads population into scenario

        reader.readFile(inputFile); //Writes population into scenario

        TinderRelocator tinderRelocator = new TinderRelocator(scenario.getPopulation(), inner, outer);
        //Call relocate method
        tinderRelocator.relocate();

        logger.info("");
        PopulationWriter writer = new PopulationWriter(tinderRelocator.getPopulation());//Writes population into scenario
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated.xml.gz");
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
     * Getter Population
     *
     * @return population variable
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * @return coordinates
     */
    private Coord coordSelector() {
        //Max and Min values
        Envelope envelope = outerGeometry.getEnvelopeInternal();

        final double maxXHome = envelope.getMaxX();
        final double minXHome = envelope.getMinX();
        final double maxYHome = envelope.getMaxY();
        final double minYHome = envelope.getMinY();

        double coordX = 0;
        double coordY = 0;
        Coord coord = new Coord(coordX, coordY);

        while (!outerGeometry.contains(MGC.coord2Point(coord)) || innerGeometry.contains(MGC.coord2Point(coord))) {
            coordX = minXHome + (Math.random() * (maxXHome - minXHome)); //Random double value between min longitude value and max latitude value
            coordY = minYHome + (Math.random() * (maxYHome - minYHome)); //Random double value between min latitude value and max latitude value
            coord = new Coord(coordX, coordY);
        }
        return coord;
    }

    /**
     * @param relocatedCell cell where it will be going to
     * @return
     */
    public Coord hardCodeCoords(int relocatedCell) {

        Coord coord1 = new Coord(338142.07, 5719759.81);
        Coord coord2 = new Coord(396731.31, 5720122.43);
        Coord coord3 = new Coord(445799.15, 5722771.49);
        Coord coord4 = new Coord(308462.43, 5699860.89);
        Coord coord5 = new Coord(374311.27, 5705428.26);
        Coord coord6 = new Coord(430345.25, 5703971.05);
        Coord coord7 = new Coord(308979.33, 5672689.09);
        Coord coord8 = new Coord(373932.04, 5674515.38);
        Coord coord0 = new Coord(434569.45, 5676445.65);

        if (relocatedCell == 1) {
            logger.info("Coord1");
            return coord1;
        } else if (relocatedCell == 2) {
            logger.info("Coord2");
            return coord2;
        } else if (relocatedCell == 3) {
            logger.info("Coord3");
            return coord3;
        } else if (relocatedCell == 4) {
            logger.info("Coord4");
            return coord4;
        } else if (relocatedCell == 5) {
            logger.info("Coord5");
            return coord5;
        } else if (relocatedCell == 6) {
            logger.info("Coord6");
            return coord6;
        } else if (relocatedCell == 7) {
            logger.info("Coord7");
            return coord7;
        } else if (relocatedCell == 8) {
            logger.info("Coord8");
            return coord8;
        } else if (relocatedCell == 0) {
            logger.info("Coord0");
            return coord0;
        } else {
            return null;
        }
    }

    /**
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
     * Relocates hard coded coordinates
     */
    public void hardCodeRelocation() {
        Coord oldHome = null;
        Activity home = null;
        int relocatedEverything = 0;
        int counter = 0;
        for (Person person : population.getPersons().values()) {
            if (fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                Plan plan = person.getSelectedPlan();
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getType().toString().contains("home") && !activity.getCoord().equals(oldHome)) {
                            activity.setCoord(hardCodeCoords(1));
                            oldHome = activity.getCoord();
                            home = activity;
                            System.out.println("Relocation completed.");
                            relocatedEverything++;
                        } else if (activity.getType().toString().contains("home") && activity.getCoord().equals(oldHome)) {
                            activity.setCoord(home.getCoord());
                        } else if (activity.getType().toString().contains("work")) {
                            activity.setCoord(home.getCoord());
                        }
                    }
                }
            } else {
                logger.info("Person_" + person.getId().toString() + " is NOT relocated.");
            }
            counter++;
        }
    }

    /**
     * Relocater class
     * does the real relocating
     */
    public void relocate() {
        int seed = 1;
        int relocatedEverything = 1;
        int relocatedHomeOnly = 1;
        //The plan for each person is gotten from the the scenario
        for (Person person : population.getPersons().values()) {
            if (fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                Plan plan = person.getSelectedPlan();
                changeEverythingInPlan(plan, seed);
                seed++;
                relocatedEverything++;
                logger.info("Person_" + person.getId().toString() + " has relocated EVERYTHING.");
            } else if (fractionOfPopulationRelocating(population, relocatedHomeOnly, relocateOnlyHomeFraction) && !fractionOfPopulationRelocating(population, relocatedEverything, relocateEverythingFraction)) {
                Plan plan = person.getSelectedPlan();
                changeOnlyHomeInPlan(plan);
                relocatedHomeOnly++;
                logger.info("Person_" + person.getId().toString() + " has relocated ONLY HOME.");
            } else {
                logger.info("Person_" + person.getId().toString() + " is NOT relocated.");
            }
        }
    }

    /**
     * @param population given from the relocate class
     * @param people     an integer which counts number of people
     * @return either true or false stating if percentage of
     * population has been relocated
     */
    public Boolean fractionOfPopulationRelocating(Population population, int people, double relocatingFrac) {
        final double fracRelocating = relocatingFrac; //given as a decimal. eg. 0.50 is 50%
        if (people <= fracRelocating * population.getPersons().size()) {
            return true;
        } else {
            return true;
        }
    }

    /**
     * This changes the plan if only the home is needed to be moved.
     *
     * @param plan for selected person is imported from the relocator method.
     */
    private void changeOnlyHomeInPlan(Plan plan) {
        Coord oldHome = null;
        Activity home = null;
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(coordSelector());
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                }
            }
        }
    }

    /**
     * @param plan is passed through
     */
    private void changeEverythingInPlan(Plan plan, int seed) {
        Coord oldHome = null;
        Activity home = null;
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(coordSelector());
                    homeArea = createCircle(activity.getCoord(), randomRadius(seed));
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                } else {
                    activity.setCoord(coordLimiter(homeArea));
                }
            }
        }
    }

    /**
     * This generates a random radius value to be used as the homeArea radii
     * Refer to : https://www.desmos.com/calculator/jbgvmtejrd
     * Eqn: y = -(0.00001x+2.15)^9+(0.000001x)^-2+1000
     * @return randomRadius
     */

    private double randomRadius(int seed) {
        final int maxWeightParam = 24816; //x-values in the graph/equation
        final int minWeightParam = 3160; //eg. 3160 value here means approx. 100000m min radius

        Random ran = new Random(seed);
        double weight = ran.nextInt(((maxWeightParam - minWeightParam) + 1)) + minWeightParam;

        double randomRadius = -Math.pow((0.00001 * weight + 2.15), 9) + Math.pow(0.000001 * weight, -2) + 1000;

        //Returns the double random radius which is generated from graph.
        return randomRadius;
    }

    /**
     * CSV Parser, parses coordinates
     *
     */

    Coord CSVCoordinatesParser(int idnum) {
        String x = null;
        String y = null;
        String id = null;
        int counter = 0;
        Iterable<CSVRecord> records;
        try (Reader in = new FileReader("/Users/nanddesai/Documents/mercator-nemo/src/relocationInput.csv")) {
            records = CSVFormat.EXCEL.parse(in);
            for (CSVRecord record : records) {
                if (counter - 1 <= idnum) {
                    id = record.get(0);
                    x = record.get(1);
                    y = record.get(2);
                    counter++;
                    //If id number asked for is larger than max number in
                    //in the list it uses the last id number in the list.
                } else {
                    break;
                }
            }
            logger.info("CSV file has been Parsed.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Coord coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
        return coord;
    }

    /**
     * This parses the probability CSV file
     *
     * @param row
     * @param col
     */
    public double CSVProbabilityParser(int originalCell, int relocationCell) {
        int col = relocationCell + 1;
        int row = originalCell + 1;
        int counter = 0;
        String str = "";
        double probability = 0;
        try (Reader in = new FileReader("/Users/nanddesai/Documents/mercator-nemo/src/probabilitiesOfRelocation.csv")) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
            for (CSVRecord record : records) {
                if (counter <= row) {
                    str = record.get(col);
                    counter++;
                } else {
                    break;
                }
            }
            probability = Double.parseDouble(str);
            logger.info("CSV file has been Parsed.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        hardCodeCoords(relocationCell);
        return probability;
    }
}