package org.matsim.nemo;

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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;


public class TinderRelocator {
    private Geometry innerGeometry;
    private Geometry outerGeometry;
    private Geometry homeArea;
    private Population population;

    /**
     * @param population
     * @param innerGeometry
     * @param outerGeometry
     */
    TinderRelocator(Population population, Geometry innerGeometry, Geometry outerGeometry) {
        this.population = population;
        this.innerGeometry = innerGeometry;
        this.outerGeometry = outerGeometry;

    }

    /**
     * @param coord
     * @param RADIUS
     * @return circle shape
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
     * @param args
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

        PopulationWriter writer = new PopulationWriter(tinderRelocator.getPopulation());//Writes population into scenario
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated.xml.gz");
    }

    /**
     * @param pathToFile
     * @return
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
    Coord coordSelector() {
        //Max and Min values
        Envelope envelope = outerGeometry.getEnvelopeInternal();

        final double maxX = envelope.getMaxX();
        final double minX = envelope.getMinX();
        final double maxY = envelope.getMaxY();
        final double minY = envelope.getMinY();

        double coordX = 0;
        double coordY = 0;
        Coord coord = new Coord(coordX, coordY);

        while (!outerGeometry.contains(MGC.coord2Point(coord)) || innerGeometry.contains(MGC.coord2Point(coord))) {
            coordX = minX + (Math.random() * (maxX - minX)); //Random double value between min longitude value and max latitude value
            coordY = minY + (Math.random() * (maxY - minY)); //Random double value between min latitude value and max latitude value
            coord = new Coord(coordX, coordY);
        }
        return coord;
    }

    /**
     * @return
     */
    Coord coordLimiter(Geometry homeArea) {
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
     * @param activity
     * @return actID
     */
    //https://stackoverflow.com/questions/18590901/check-if-a-string-contains-numbers-java
    private double getID(String activity) {
        if (activity == null || activity.isEmpty()) System.out.println("Error in reading activity.");

        boolean numFound = false;

        StringBuilder stringBuilder = new StringBuilder();
        double actID = 0;
        for (char c : activity.toCharArray()) {
            if (Character.isDigit(c)) {
                stringBuilder.append(c);
                numFound = true;
            } else if (numFound) {
                break;
            }
        }
        actID = Double.parseDouble(stringBuilder.toString());
        return actID;
    }

    /**
     * Relocater class
     */
    public void relocate() {
        boolean homeSelected = false;
        int seed = 1;
        //The plan for each person is gotten from the the scenario
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            changePlan(plan, seed);
            seed++;
        }
    }

    /**
     * @param plan
     */
    private void changePlan(Plan plan, int seed) {
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
                    System.out.println("\nCentral home coordinates relocated successfully!");
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                    System.out.println("Back to home.");
                } else {
                    activity.setCoord(coordLimiter(homeArea));
                    System.out.println("Non-central activity coordinates relocated successfully, within radius!");
                }
            }
        }
    }

    /**
     * This generates a random radius value to be used as the homeArea radii
     *
     * @return randomRadius
     */

    private double randomRadius(int seed) {
        final int weightParam = 24816;
        Random ran = new Random(seed);
        double weight = ran.nextInt(((weightParam - 1400) + 1) + 1400);

        double randomRadius = -Math.pow((0.00001 * weight + 2.15), 9) + Math.pow(0.000001 * weight, -2) + 1000;

        System.out.println("\nRadius: " + randomRadius + " meters.");

        //Returns the double random radius which is generated from graph.
        return randomRadius;
    }

}
