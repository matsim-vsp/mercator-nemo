/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.nemo.runners;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.gbl.MatsimRandom;

import java.util.List;
import java.util.Random;

/**
 * A default implementation of {@link RaptorIntermodalAccessEgress} returning a new RIntermodalAccessEgress,
 * which contains a list of legs (same as in the input), the associated travel time as well as the disutility.
 *
 * @author pmanser / SBB
 */
public class NemoRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {

	Config config;
	DrtFaresConfigGroup drtFaresConfigGroup;

	Random random = MatsimRandom.getLocalInstance();

	@Inject
	NemoRaptorIntermodalAccessEgress(Config config) {
		this.config = config;
		this.drtFaresConfigGroup = ConfigUtils.addOrGetModule(config, DrtFaresConfigGroup.class);
	}

	@Override
	public RIntermodalAccessEgress calcIntermodalAccessEgress(final List<? extends PlanElement> legs, RaptorParameters params, Person person, RaptorStopFinder.Direction direction) {
		// maybe nicer using raptor parameters per person ?
		String subpopulationName = null;
		if (person.getAttributes() != null) {
			Object attr = person.getAttributes().getAttribute("subpopulation");
			subpopulationName = attr == null ? null : attr.toString();
		}

		ScoringParameterSet scoringParams = config.planCalcScore().getScoringParameters(subpopulationName);

		double utility = 0.0;
		double tTime = 0.0;
		for (PlanElement pe : legs) {
			if (pe instanceof Leg) {
				String mode = ((Leg) pe).getMode();
				var travelTime = ((Leg) pe).getTravelTime();

				// overrides individual parameters per person; use default scoring parameters
				if (travelTime.isUndefined()) {
					tTime += travelTime.seconds();
					utility += travelTime.seconds() * (scoringParams.getModes().get(mode).getMarginalUtilityOfTraveling() + (-1) * scoringParams.getPerforming_utils_hr()) / 3600;
				}
				double distance = ((Leg) pe).getRoute().getDistance();
				if (distance != 0.) {
					utility += distance * scoringParams.getModes().get(mode).getMarginalUtilityOfDistance();
					utility += distance * scoringParams.getModes().get(mode).getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney();
				}
				utility += scoringParams.getModes().get(mode).getConstant();

				// account for drt fares
				for (DrtFareConfigGroup drtFareConfigGroup : drtFaresConfigGroup.getDrtFareConfigGroups()) {
					if (drtFareConfigGroup.getMode().equals(mode)) {
						double fare = 0.;
						if (distance != 0.) {
							fare += drtFareConfigGroup.getDistanceFare_m() * distance;
						}

						if (travelTime.isUndefined()) {
							fare += drtFareConfigGroup.getTimeFare_h() * travelTime.seconds() / 3600.;

						}

						fare += drtFareConfigGroup.getBasefare();
						fare = Math.max(fare, drtFareConfigGroup.getMinFarePerTrip());
						utility += -1. * fare * scoringParams.getMarginalUtilityOfMoney();
					}
				}

			}
		}
		return new RIntermodalAccessEgress(legs, -utility, tTime, direction);
	}
}
