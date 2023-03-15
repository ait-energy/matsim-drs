package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public int cellSize = 800;
    public int neighbourhoodSize = 30;
    public double riderDepartureTimeAdjustment = 0.25 * 60 * 60;
    public int timeSegmentLength = 2 * 60 * 60;

    // MaxDetourFactor is a function of the trip total travel time
    // MaxDetourFactor = K-m(TotalTravelTime)
    public double constant = 1.7;
    public double slope = 0.01;

    public double driverMoneyPerKM = 0.2;

    public double riderMoneyPerKM = 0;

    public CarpoolingConfigGroup(String name) {
        super(name);
    }

}
