package at.ac.ait.matsim.dominoridesharing.run;

import org.checkerframework.checker.units.qual.A;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.ArrayList;

public class CarpoolingConfigGroup extends ReflectiveConfigGroup {


    public double driverMaxWaitTime =  Double.NaN;// [s]

    public double driverMaxTravelTimeToPassenger =  Double.NaN;// [s]

    public double passengerMaxWaitTime =  Double.NaN;// [s]

    public double maxDistance =  Double.NaN;// [m]

    public double detourFactorWeight =  Double.NaN;

    public double driverWaitingTimeWeight =  Double.NaN;

    public double passengerWaitingTimeWeight=  Double.NaN;



    public CarpoolingConfigGroup(String name) {
        super(name);
    }

    public CarpoolingConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
        super(name, storeUnknownParametersAsStrings);
    }
}
