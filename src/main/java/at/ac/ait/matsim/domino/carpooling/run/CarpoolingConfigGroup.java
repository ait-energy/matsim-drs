package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public int cellSize = 800; //in
    public int neighbourhoodSize = 30;
    public double riderDepartureTimeAdjustment = 0.01*60*60;
    public int timeSegmentLength = 2*60*60;
    public double maxDetourFactor = 1.5;



    public CarpoolingConfigGroup(String name) {
        super(name);
    }

}
