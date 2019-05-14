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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;


public class TinderRelocator {
    Coord home = new Coord(0, 0);
    Coord work = new Coord(0, 0);
    Coord ed = new Coord(0, 0);
    Coord shopping = new Coord(0, 0);
    Coord other = new Coord(0, 0);
    Coord leisure = new Coord(0, 0);
    private Geometry innerGeometry;
    private Geometry outerGeometry;
    private Population population;
    private double homeId = 0;
    private double workId = 0;
    private double edId = 0;
    private double shoppingId = 0;
    private double otherId = 0;
    private double leisureId = 0;
    private double actId = 0;

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

        while (!outerGeometry.contains(MGC.coord2Point(coord)) || innerGeometry.contains(MGC.coord2Point(coord))) {
            coordX = minX + (Math.random() * (maxX - minX)); //Random double value between min longitude value and max latitude value
            coordY = minY + (Math.random() * (maxY - minY)); //Random double value between min latitude value and max latitude value
            coord = new Coord(coordX, coordY);
            System.out.println("Coordinates changed successfully!");
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
     * @param activity
     */
    private void checkId(Activity activity) {
        Geometry homeArea = createCircle(home, 10000);

        if (activity.getType().contains("interaction")) {
            activity.setCoord(coordLimiter(homeArea));//X and Y coordinates for new coord
            System.out.println("Activity " + activity.getType() + " has been relocated.");
        } else {
            actId = getID(activity.getType());
            if (activity.getType().contains("home_")) { //home, always start at home
                if (homeId != actId) {
                    activity.setCoord(coordSelector());
                    home = activity.getCoord();
                    homeId = actId;
                    System.out.println("HomeID: " + homeId);
                } else {
                    System.out.println("Back home");
                    activity.setCoord(home);
                }
                System.out.println("Home " + activity.getType() + " has been relocated.");
            } else if (activity.getType().contains("work_")) { //work
                if (workId != actId) {
                    activity.setCoord(coordLimiter(homeArea));
                    work = activity.getCoord();
                    workId = actId;
                    System.out.println("WorkId: " + workId);
                } else {
                    System.out.println("Back at Work");
                    activity.setCoord(work);
                }
            } else if (activity.getType().contains("education_")) { //education
                if (edId != actId) {
                    activity.setCoord(coordLimiter(homeArea));
                    ed = activity.getCoord();
                    edId = actId;
                    System.out.println("EduId: " + edId);
                } else {
                    System.out.println("Back at School.");
                    activity.setCoord(ed);
                }
            } else if (activity.getType().contains("leisure_")) { //Leisure
                if (leisureId != actId) {
                    activity.setCoord(coordLimiter(homeArea));
                    leisure = activity.getCoord();
                    leisureId = actId;
                    System.out.println("LeisureId: " + leisureId);
                } else {
                    System.out.println("Back at Leisure");
                    activity.setCoord(leisure);
                }
            } else if (activity.getType().contains("shopping_")) { //Shopping
                if (shoppingId != actId) {
                    activity.setCoord(coordLimiter(homeArea));
                    shopping = activity.getCoord();
                    shoppingId = actId;
                    System.out.println("ShoppingId: " + shoppingId);
                } else {
                    System.out.println("Back at Shopping");
                    activity.setCoord(shopping);
                }
            } else if (activity.getType().contains("other_")) { //other
                if (otherId != actId) {
                    activity.setCoord(coordLimiter(homeArea));
                    other = activity.getCoord();
                    otherId = actId;
                    System.out.println("OtherId: " + otherId);
                } else {
                    System.out.println("Back at Other");
                    activity.setCoord(other);
                }
            }
        }
    }

    /**
     * @param plan
     * @return
     */
    private HashMap<Integer, Activity> generalizeActivity(Plan plan) {
        double euclideanDistances = 0;
        int counter1 = 0;
        int counter2 = 0;
        HashMap<Integer, Activity> map = new HashMap<>();
        Coord temp = new Coord(0, 0);

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity acti = (Activity) planElement;
                map.put(counter1, acti); //Make map of keys
                counter1++;
            }
        }
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity act = (Activity) planElement;
                for (Activity value : map.values()) {
                    euclideanDistances = CoordUtils.calcEuclideanDistance(temp, act.getCoord());
                    if (euclideanDistances <= 1) {
                        if (map.get(counter2).getType().contains("home_")) {
                            map.get(counter2).setType(act.getType());
                        }
                        System.out.println("Duplicate removed.");
                    }
                    temp = value.getCoord();
                    counter2++;
                }
            }
            counter2 = 0;
        }

        return map;
    }

    /**
     * Relocater class
     */
    private void relocate() {
        HashMap<Integer, Activity> map;
        //The plan for each person is gotten from the the scenario
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            map = generalizeActivity(plan);
            System.out.println("Start of plan. ------------------------------");
            for (Activity value : map.values()) {
                checkId(value);
            }
            System.out.println("End of plan. ------------------------------");
        }
    }


}
