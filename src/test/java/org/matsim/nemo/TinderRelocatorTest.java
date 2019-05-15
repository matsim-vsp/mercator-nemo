package org.matsim.nemo;

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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.matsim.api.core.v01.TransportMode.car;

public class TinderRelocatorTest {
    Coord home;
    Coord work;
    private Geometry innerGeometry;
    private Geometry outerGeometry;
    private Geometry homeArea;

    private static Geometry getFirstGeometryFromShapeFile(Path pathToFile) {
        for (SimpleFeature feature : ShapeFileReader.getAllFeatures(pathToFile.toString())) {
            return (Geometry) feature.getDefaultGeometry();
        }
        throw new RuntimeException("Runtime exception/error, geometry is broken. Unexpected Error.");
    }

    private static Geometry createCircle(Coord coord, final double RADIUS) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(coord.getX(), coord.getY()));
        shapeFactory.setSize(RADIUS * 2);
        return shapeFactory.createCircle();
    }

    @Test
    public void relocateOne() {
        Path shapeRuhrgebiet = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp");
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry inner = getFirstGeometryFromShapeFile(shapeRuhrgebiet);
        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        innerGeometry = inner;
        outerGeometry = outer;

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        Person person = population.getFactory().createPerson(Id.createPersonId(1));
        Plan plan = population.getFactory().createPlan();

        Activity homeActivityInTheMorning = population.getFactory().createActivityFromCoord("home", home);
        homeActivityInTheMorning.setStartTime(9 * 60 * 60);
        homeActivityInTheMorning.setCoord(coordSelector());
        plan.addActivity(homeActivityInTheMorning);

        Leg toWork = population.getFactory().createLeg(car);
        plan.addLeg(toWork);

        Activity workActivity = population.getFactory().createActivityFromCoord("work_1", work);
        workActivity.setEndTime(17 * 60 * 60);
        workActivity.setCoord(coordSelector());
        plan.addActivity(workActivity);

        Leg toHome = population.getFactory().createLeg(car);
        plan.addLeg(toHome);

        Activity homeActivityInTheEvening = population.getFactory().createActivityFromCoord("home", home);
        homeActivityInTheEvening.setStartTime(17 * 60 * 60);
        homeActivityInTheEvening.setCoord(homeActivityInTheMorning.getCoord());
        plan.addActivity(homeActivityInTheEvening);

        person.addPlan(changePlan(plan));
        population.addPerson(person);

        PopulationWriter writer = new PopulationWriter(population);//Writes population into scenario
        writer.write("/Users/nanddesai/Documents/NEMOProject/outputPath/population_relocated.xml.gz");
    }

    private Plan changePlan(Plan plan) {
        Coord oldHome = null;
        Activity home = null;
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.getType().contains("home") && !activity.getCoord().equals(oldHome)) {
                    oldHome = activity.getCoord();
                    activity.setCoord(coordSelector());
                    homeArea = createCircle(activity.getCoord(), 10000);
                    home = activity;
                } else if (activity.getType().contains("home") && activity.getCoord().equals(oldHome)) {
                    activity.setCoord(home.getCoord());
                    System.out.println("Back Home.");
                } else {
                    activity.setCoord(coordLimiter(homeArea));
                    this.work = activity.getCoord();
                }
            }
        }
        return plan;
    }

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
}