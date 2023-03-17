package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    //Inspired by Taxi contrib : Limits the number of possible riders requests considered for a driver during
    //the matching process.
    //Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results.
    //To turn off this feature - specify a sufficiently big number (not recommended), the default value is 30.
    public int neighbourhoodSize = 30;


    //Inspired by Taxi contrib : The side length of square zones used in zonal registers of riders requests.
    //The default value is 1000 m. This value is good for urban areas. For large areas with sparsely
    //distributed population and low carpooling share, you may consider using a bigger cell size
    //On the other hand, if neighbourhoodSize is very low, a smaller cell size may work better.
    public int cellSize = 1000;


    //The duration of the time segments used in time segment registers of riders requests.
    //The default value is dividing the day by 2 hour segments. The before and after segments are checked as well.
    //This is more realistic for the cases that a driver and a rider departure times are
    //very close but both are at the edges of two different segments.
    public int timeSegmentLength = 2 * 60 * 60;


    //The amount of time the riders are willing to adjust their departure times.
    //During the matching process, the arrival of driver to pick-up point is checked
    //whether it is within the rider departure time +- the riderDepartureTimeAdjustment.
    public double riderDepartureTimeAdjustment = 0.25 * 60 * 60;


    //The components of the max detour factor as a function of total travel time.
    //The function is currently linear (MaxDetourFactor = -m*(TotalTravelTime)+k").
    //However the function can be further extended into any other polynomial function.
    public double constant = 1.7;
    public double slope = 0.01;


    //The amount of money (in cents) per kilometre the driver gains when picking a rider
    public double driverMoneyPerKM = 0.2;

    //The amount of money (in cents) per kilometre the rider gains when being picked up by a driver.
    public double riderMoneyPerKM = 0;

    public CarpoolingConfigGroup(String name) {
        super(name);
    }

}
