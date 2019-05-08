package org.matsim.nemo;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class BikeLinkSpeedCalculator implements LinkSpeedCalculator {

    static final String BIKE_SPEED_FACTOR_KEY = "bike_speed_factor";

    @Override
    public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {

        if (isBike(qVehicle))
            return getMaximumVelocityForBike(qVehicle, link, time);
        else
            return getDefaultMaximumVelocity(qVehicle, link, time);
    }

    private double getMaximumVelocityForBike(QVehicle qVehicle, Link link, double time) {
        try {
            double speedFactor = (double) link.getAttributes().getAttribute(BIKE_SPEED_FACTOR_KEY);
            return Math.min(qVehicle.getMaximumVelocity() * speedFactor, link.getFreespeed(time));
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve speed factor from link. Set the speed factor for bikes on each link of the network if you want to use BikLinkSpeedCalculator!");
        }
    }

    private double getDefaultMaximumVelocity(QVehicle qVehicle, Link link, double time) {
        return Math.min(qVehicle.getMaximumVelocity(), link.getFreespeed(time));
    }

    private boolean isBike(QVehicle qVehicle) {
        return qVehicle.getVehicle().getType().getId().toString().equals(TransportMode.bike);
    }
}
