package org.matsim.nemo;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuhrAgentsFilterTest {

    @Test
    public void test() {

        SimpleFeature feature = createSquare();
        Scenario scenario = createScenario();

        RuhrAgentsFilter filter = new RuhrAgentsFilter(scenario, Collections.singletonList(feature));

        assertTrue(filter.includeAgent(Id.createPersonId("within-bounds")));
        assertFalse(filter.includeAgent(Id.createPersonId("not-within-bounds")));
        assertFalse(filter.includeAgent(Id.createPersonId("somewhere-not-home")));
        assertFalse(filter.includeAgent(Id.createPersonId("without-plan")));
    }

    private SimpleFeature createSquare() {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(10, 10), new Coordinate(0, 10), new Coordinate(0, 0)
        };
        Polygon square = geometryFactory.createPolygon(coords);

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("some-name");
        builder.add("geometry", Geometry.class);
        builder.setCRS(DefaultGeocentricCRS.CARTESIAN);
        SimpleFeatureType type = builder.buildFeatureType();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        featureBuilder.set("geometry", square);
        return featureBuilder.buildFeature("test-feature");
    }

    private Scenario createScenario() {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        scenario.getPopulation().addPerson(createPerson("not-within-bounds", "home", new Coord(100, 100), scenario.getPopulation().getFactory()));
        scenario.getPopulation().addPerson(createPerson("within-bounds", "home", new Coord(5, 5), scenario.getPopulation().getFactory()));
        scenario.getPopulation().addPerson(createPerson("not-at-home-first", "somewhere-not-home", new Coord(5, 5), scenario.getPopulation().getFactory()));
        scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.createPersonId("without-plan")));

        return scenario;
    }

    private Person createPerson(String id, String actType, Coord coord, PopulationFactory factory) {

        Plan plan = factory.createPlan();
        Activity homeActivity = factory.createActivityFromCoord(actType, coord);
        plan.addActivity(homeActivity);

        Person person = factory.createPerson(Id.createPersonId(id));
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        return person;
    }

}