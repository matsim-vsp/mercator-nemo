package org.matsim.nemo;

import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.matsim.api.core.v01.TransportMode.car;

public class TinderRelocatorTest {
    private static Geometry innerGeometry;
    private static Geometry outerGeometry;

    private static Geometry getFirstGeometryFromShapeFile(Path pathToFile) {
        for (SimpleFeature feature : ShapeFileReader.getAllFeatures(pathToFile.toString())) {
            return (Geometry) feature.getDefaultGeometry();
        }
        throw new RuntimeException("Runtime exception: error, geometry is broken. Unexpected Error.");
    }

    /**
     * Setting up the class
     */

    @BeforeClass
    public static void setupClass() {
        Path shapeRuhrgebiet = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp");
        Path shapeLimits = Paths.get("/Users/nanddesai/nemo_mercator/data/original_files/shapeFiles/sourceShape_NRW/sourceShape_NRW/dvg2bld_nw.shp");

        Geometry inner = getFirstGeometryFromShapeFile(shapeRuhrgebiet);
        Geometry outer = getFirstGeometryFromShapeFile(shapeLimits);

        innerGeometry = inner;
        outerGeometry = outer;
    }

    /**
     * Testing if one relocation occurs
     */
    @Test
    public void relocateOne() {
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 0);

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        Person person = population.getFactory().createPerson(Id.createPersonId(1));
        Plan plan = population.getFactory().createPlan();

        Activity homeActivityInTheMorning = population.getFactory().createActivityFromCoord("home", home);
        homeActivityInTheMorning.setStartTime(9 * 60 * 60);
        plan.addActivity(homeActivityInTheMorning);

        Leg toWork = population.getFactory().createLeg(car);
        plan.addLeg(toWork);

        Activity workActivity = population.getFactory().createActivityFromCoord("work_1", work);
        workActivity.setEndTime(17 * 60 * 60);
        plan.addActivity(workActivity);

        Leg toHome = population.getFactory().createLeg(car);
        plan.addLeg(toHome);

        Activity homeActivityInTheEvening = population.getFactory().createActivityFromCoord("home", home);
        homeActivityInTheEvening.setStartTime(17 * 60 * 60);
        plan.addActivity(homeActivityInTheEvening);

        person.addPlan(plan);
        population.addPerson(person);

        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);
        tinderRelocator.relocate();

        assertTrue("AssertionFalse: More than one person was created in the population.", population.getPersons().size() == 1);

        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.toString().contains("home")) {
                    assertFalse("Home was not relocated.", home.equals(activity.getCoord()));
                } else {
                    assertFalse("Work was not relocated.", work.equals(activity.getCoord()));
                }
            }
        }

    }

    /**
     * Testing if multiple relocations occur
     */
    @Test
    public void relocateMultiple() {
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 100);

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 0; i < 100; i++) {
            population.addPerson(createPerson(population, home, work, i));
        }

        tinderRelocator.relocate();

        for (PlanElement planElement : population.getPersons().values().iterator().next().getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if (activity.toString().contains("home")) {
                    assertFalse("Home was not relocated.", home.equals(activity.getCoord()));
                } else {
                    assertFalse("Work was not relocated.", work.equals(activity.getCoord()));
                }
            }
        }

        assertTrue("AssertionFalse: " + population.getPersons().size() + " person were created. Incorrect size of population.", population.getPersons().size() == 100);
        System.out.println("\n---MultipleRelocationTestPassed.---");
    }

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
}