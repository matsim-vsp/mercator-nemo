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
            System.out.println("Coordinates changed successfully!");
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
            System.out.println("Coordinates limited and changed successfully!");
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
        //The plan for each person is gotten from the the scenario
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            changePlan(plan);
        }
    }

    /**
     * @param plan
     */
    private void changePlan(Plan plan) {
        Coord oldHome = null;
        Activity home = null;
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(coordSelector());
                    homeArea = createCircle(activity.getCoord(), randomRadius());
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                    System.out.println("Back Home.");
                } else {
                    activity.setCoord(coordLimiter(homeArea));
                }
            }
        }
    }

    private Double randomRadius() {
        final int weightParam = 100;
        final int radiusMaxParam = 100000;

        int weight = (int) Math.round(1 + (Math.random() * (weightParam - 1)));
        double randomRadius;

        if (weight >= weightParam * 0.6) {
            randomRadius = radiusMaxParam * 0.01 + (Math.random() * (radiusMaxParam * 0.02 - radiusMaxParam * 0.01));
            System.out.println("Rad: " + randomRadius + " m.");
        } else if (weight >= weightParam * 0.3) {
            randomRadius = radiusMaxParam * 0.02 + (Math.random() * (radiusMaxParam * 0.1 - radiusMaxParam * 0.02));
            System.out.println("Rad: " + randomRadius + " m.");
        } else if (weight >= weightParam * 0.1) {
            randomRadius = radiusMaxParam * 0.1 + (Math.random() * (radiusMaxParam * 0.2 - radiusMaxParam * 0.1));
            System.out.println("Rad: " + randomRadius + " m.");
        } else if (weight >= weightParam * 0.03) {
            randomRadius = radiusMaxParam * 0.2 + (Math.random() * (radiusMaxParam * 0.5 - radiusMaxParam * 0.2));
            System.out.println("Rad: " + randomRadius + " m.");
        } else if (weight >= 0) {
            randomRadius = radiusMaxParam * 0.5 + (Math.random() * (radiusMaxParam * 1 - radiusMaxParam * 0.5));
            System.out.println("Rad: " + randomRadius + " m.");
        } else {
            System.out.println("RandomNumGenerationError: Error should not occur.");
            randomRadius = -1;
        }

        return randomRadius;
    }

}
