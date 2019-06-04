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
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.matsim.api.core.v01.TransportMode.car;

public class TinderRelocatorTest {
    private static Geometry innerGeometry;
    private static Geometry outerGeometry;
    private static Logger logger = Logger.getLogger("TinderRelocatorTest");

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
        System.out.println("----RelocationOfOneTestBegin----");
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
        logger.info("---OneRelocationTestPassed.---");

    }

    /**
     * Testing if multiple relocations occur
     */
    @Test
    public void relocateMultiple() {
        logger.info("----MultipleRelocationTestBegin----");
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 100);

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 1; i <= 100; i++) {
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
        logger.info("---MultipleRelocationTestPassed.---");
    }

    /**
     * Testing if we can move a percentage of the population
     * Population consists of moving everything home+other activities.
     */
    @Test
    public void relocateEverythingOfFractionOfPopulation() {
        logger.info("----FractionalRelocationTestBegin----");
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 100);

        int numOfPeople = 1000;
        int relocatedPeople = 0;

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 1; i <= numOfPeople; i++) {
            population.addPerson(createPerson(population, home, work, i));
        }

        tinderRelocator.relocate();

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().toString().contains("home") && !activity.getCoord().equals(home)) {
                        relocatedPeople++;
                    }
                }
            }
        }
        logger.info(relocatedPeople / 2 + " people were relocated.");
        assertTrue("The percentage of people relocated is incorrect.", tinderRelocator.fractionOfPopulationRelocating(population, relocatedPeople / 2, tinderRelocator.relocateEverythingFraction));
        assertTrue("AssertionFalse: " + population.getPersons().size() + " person were created. Incorrect size of population.", population.getPersons().size() == 1000);
        assertEquals("The relocated number of people is not equal to the ratio expected.", (float) tinderRelocator.relocateEverythingFraction, (float) (relocatedPeople / 2) / numOfPeople, 0);
        logger.info("----FractionalRelocationTestPassed.----");
    }

    /**
     * Tests if the relocation of homes only can happen fractionally
     */
    @Test
    public void relocateFractionOfPopulationHomeOnly() {
        logger.info("----FractionalRelocationOfHomeOnlyTestBegin----");
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 100);

        int numOfPeople = 1000;
        int relocatedHomes = 0;
        int relocatedEverything = 0;
        int otherActivities = 0;

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 1; i <= numOfPeople; i++) {
            population.addPerson(createPerson(population, home, work, i));
        }

        tinderRelocator.relocate();

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().toString().contains("home") && !activity.getCoord().equals(home)) {
                        relocatedHomes++;
                    }
                }
            }
        }

        logger.info(relocatedHomes / 2 + " people were relocated.");
        assertTrue("The percentage of homes relocated is incorrect.", tinderRelocator.fractionOfPopulationRelocating(population, relocatedHomes / 2, tinderRelocator.relocateOnlyHomeFraction));
        assertTrue("AssertionFalse: " + population.getPersons().size() + " person were created. Incorrect size of population.", population.getPersons().size() == 1000);
        assertEquals("The relocated number of people is not equal to the ratio expected.", (float) tinderRelocator.relocateOnlyHomeFraction, (float) (relocatedHomes / 2) / numOfPeople, 0);
        logger.info("----FractionalRelocationOfHomeOnlyTestPassed.----");
    }

    /**
     * Tests if multiple parameter changes can occur.
     * Some people want to move everything out of the Ruhr.
     * Some people want only to move their homes out of the Ruhr.
     */
    @Test
    public void relocatePopulationWithMultipleFractionalParams() {
        logger.info("----MultipleParameterFractionalRelocationBegin----");
        Coord home = new Coord(0, 0);
        Coord work = new Coord(100, 100);

        int numOfPeople = 1000;
        int relocatedHomesOnly = 0;
        int noRelocation = 0;
        int otherActivities = 0;
        int relocatedEverything = 0;

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 1; i <= numOfPeople; i++) {
            population.addPerson(createPerson(population, home, work, i));
        }

        tinderRelocator.relocate();

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().toString().contains("home") && activity.getCoord().equals(home)) {
                        noRelocation++;
                    }
                    if (!activity.getType().toString().contains("home") && !activity.getCoord().equals(work)) {
                        otherActivities++;
                    }
                }
            }
            if (otherActivities > 0) {
                relocatedEverything++;
            }
            otherActivities = 0;
        }
        relocatedHomesOnly = numOfPeople - relocatedEverything - noRelocation / 2;

        logger.info(relocatedHomesOnly + " people relocated only their homes.");
        logger.info(relocatedEverything + " people relocated everything along with their homes.");
        assertEquals("The relocated number of people who only relocated home is not equal to the ratio expected.",
                (double) tinderRelocator.relocateOnlyHomeFraction, (double) (relocatedHomesOnly) / numOfPeople, 0.1); //giving it a 0.1 delta enables populations such as 1001
        assertEquals("The relocated number of people who relocated everything is not equal to the ratio expected.",
                (double) tinderRelocator.relocateEverythingFraction, (double) (relocatedEverything) / numOfPeople, 0.1);
        assertTrue("AssertionFalse: " + population.getPersons().size() + " person were created. Incorrect size of population.", population.getPersons().size() == 1000);
        logger.info("----MultipleParameterFractionalRelocationPassed----");
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

    @Test
    public void testingParser() {
        Coord home = new Coord(0, 0);
        Coord work = new Coord(1, 1);

        int numOfPeople = 9;

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        TinderRelocator tinderRelocator = new TinderRelocator(population, innerGeometry, outerGeometry);

        for (int i = 0; i <= numOfPeople; i++) {
            population.addPerson(createPerson(population, home, work, i));
        }

        tinderRelocator.hardCodeRelocation();
    }

}