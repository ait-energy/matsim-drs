package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public double driverMaxWaitTime = 600;// [s]

    public double driverMaxTravelTimeToPassenger = 600;// [s]

    public double passengerMaxWaitTime = 600;// [s]

    public double maxDistance = 10000;// [m]

    public double detourFactorWeight = 0.5;

    public double driverWaitingTimeWeight = 0.5;

    public double passengerWaitingTimeWeight = 0.5;

    public CarpoolingConfigGroup(String name) {
        super(name);
    }

}
