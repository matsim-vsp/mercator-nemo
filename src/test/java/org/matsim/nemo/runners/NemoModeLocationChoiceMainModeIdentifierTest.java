package org.matsim.nemo.runners;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NemoModeLocationChoiceMainModeIdentifierTest {

	@Test
	public void identifyMainMode_carTrip() {

		List<PlanElement> elements = new ArrayList<>();
		elements.add(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("car interaction", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.car));
		elements.add(PopulationUtils.createActivityFromLinkId("car interaction", Id.createLinkId("work-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link")));

		MainModeIdentifier testObject = new NemoModeLocationChoiceMainModeIdentifier();

		String mainMode = testObject.identifyMainMode(elements);

		assertEquals(TransportMode.car, mainMode);
	}

	@Test
	public void identifyMainMode_ptTrip() {

		List<PlanElement> elements = new ArrayList<>();
		elements.add(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("pt interaction", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.pt));
		elements.add(PopulationUtils.createActivityFromLinkId("pt interaction", Id.createLinkId("work-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link")));

		MainModeIdentifier testObject = new NemoModeLocationChoiceMainModeIdentifier();

		String mainMode = testObject.identifyMainMode(elements);

		assertEquals(TransportMode.pt, mainMode);
	}

	@Test
	public void identifyMainMode_multiModal() {

		List<PlanElement> elements = new ArrayList<>();
		elements.add(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("pt interaction", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.pt));
		elements.add(PopulationUtils.createActivityFromLinkId("pt interaction", Id.createLinkId("pt-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("car interaction", Id.createLinkId("pt-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.car));
		elements.add(PopulationUtils.createActivityFromLinkId("car interaction", Id.createLinkId("car-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("bike interaction", Id.createLinkId("bike-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.bike));
		elements.add(PopulationUtils.createActivityFromLinkId("bike interaction", Id.createLinkId("work-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.non_network_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link")));

		MainModeIdentifier testObject = new NemoModeLocationChoiceMainModeIdentifier();

		String mainMode = testObject.identifyMainMode(elements);

		assertEquals(TransportMode.pt, mainMode);
	}

	@Test
	public void identifyMainMode_ptTripOnlyTransitWalk() {

		List<PlanElement> elements = new ArrayList<>();
		elements.add(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(TransportMode.transit_walk));
		elements.add(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link")));

		MainModeIdentifier testObject = new NemoModeLocationChoiceMainModeIdentifier();

		String mainMode = testObject.identifyMainMode(elements);

		assertEquals(TransportMode.walk, mainMode);
	}

	@Test
	public void identifyMainMode_unknownMode() {

		final String mode = "some-mode";
		List<PlanElement> elements = new ArrayList<>();
		elements.add(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("home-link")));
		elements.add(PopulationUtils.createLeg(mode));
		elements.add(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link")));

		MainModeIdentifier testObject = new NemoModeLocationChoiceMainModeIdentifier();

		String mainMode = testObject.identifyMainMode(elements);

		assertEquals(mode, mainMode);
	}
}