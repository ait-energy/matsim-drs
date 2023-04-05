package at.ac.ait.matsim.domino.carpooling.run;

import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.utils.misc.StringUtils;

import at.ac.ait.matsim.domino.carpooling.replanning.SubtourModeChoiceForCarpooling;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "carpooling";

    public static final String CELL_SIZE = "cellSize";
    private int cellSize = 800;

    public static final String NEIGHBOURHOOD_SIZE = "neighbourhoodSize";
    private int neighbourhoodSize = 30;

    public static final String RIDER_DEPARTURE_TIME_ADJUSTMENT = "riderDepartureTimeAdjustment";
    private double riderDepartureTimeAdjustment = 0.25 * 60 * 60;

    public static final String TIME_SEGMENT_LENGTH = "timeSegmentLength";
    private int timeSegmentLength = 2 * 60 * 60;

    public static final String MAX_DETOUR_FACTOR_CONSTANT = "maxDetourFactorConstant";
    public double maxDetourFactorConstant = 1.7;

    public static final String MAX_DETOUR_FACTOR_SLOPE = "maxDetourFactorSlope";
    public double maxDetourFactorSlope = 0.01;

    public static final String PICKUP_WAITING_TIME = "pickupWaitingTime";
    public double pickupWaitingTime = 0;

    public static final String DRIVER_PROFIT_PER_KM = "driverProfitPerKm";
    private double driverProfitPerKm = 0;

    public static final String RIDER_FARE_PER_KM = "riderFarePerKm";
    private double riderFarePerKm = 0;

    public static final String MOBILITY_GUARANTEE = "mobilityGuarantee";
    private boolean mobilityGuarantee = false;
    public static final String SUBTOUR_MODE_CHOICE_MODES = "subtourModeChoiceModes";
    private String[] subtourModeChoiceModes = { TransportMode.car, Carpooling.DRIVER_MODE, Carpooling.RIDER_MODE,
            TransportMode.pt, TransportMode.bike, TransportMode.walk };

    public static final String SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES = "subtourModeChoiceChainBasedModes";
    private String[] subtourModeChoiceChainBasedModes = new String[] { TransportMode.car, Carpooling.DRIVER_MODE,
            TransportMode.bike };

    public CarpoolingConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(CELL_SIZE,
                "The side length of square zones in meters used in zonal registers of riders requests. The default value is good for urban areas. For large areas with sparsely distributed population and low carpooling share, you may consider using a bigger cell size. On the other hand, if neighbourhoodSize is very low, a smaller cell size may work better. (inspired by taxi contrib)");
        map.put(NEIGHBOURHOOD_SIZE,
                "Limits the number of possible riders requests considered for a driver during the matching process. Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results. To turn off this feature specify a sufficiently big number (not recommended). (inspired by taxi contrib)");
        map.put(RIDER_DEPARTURE_TIME_ADJUSTMENT,
                "The amount of time the riders are willing to adjust their departure times. During the matching process, the arrival of driver to pick-up point is checked whether it is within the rider departure time +- the riderDepartureTimeAdjustment.");
        map.put(TIME_SEGMENT_LENGTH,
                "The duration of the time segments used in time segment registers of riders requests. To avoid scenarios where a driver and a rider departure time are close but cross a segment boundary candidate requests are token not only from the current segment but also from the one before and after.");
        map.put(MAX_DETOUR_FACTOR_CONSTANT,
                "Component of the function to determine the maximum detour a driver is willing to take. The function is currently linear: maxDetourFactor = constant - slope * travelTime");
        map.put(MAX_DETOUR_FACTOR_SLOPE,
                "Component of the function to determine the maximum detour a driver is willing to take. The function is currently linear: maxDetourFactor = constant - slope * travelTime");
        map.put(PICKUP_WAITING_TIME,
                "The amount of minutes the driver is expected to wait till the rider enters the vehicle.");
        map.put(DRIVER_PROFIT_PER_KM,
                "The amount of money per kilometre the driver gains when picking a rider");
        map.put(RIDER_FARE_PER_KM,
                "The amount of money per kilometre the rider pays when being picked up by a driver.");
        map.put(MOBILITY_GUARANTEE,
                "The possibility for the unmatched riders to be teleported to their destination");
        map.put(SUBTOUR_MODE_CHOICE_MODES,
                "Defines all modes available for the '" + SubtourModeChoiceForCarpooling.STRATEGY_NAME
                        + "' strategy, including chain-based modes, separated by commas");
        map.put(SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES,
                "Defines the chain-based modes for the'" + SubtourModeChoiceForCarpooling.STRATEGY_NAME
                        + "' strategy, separated by commas");
        return map;
    }

    @StringGetter(CELL_SIZE)
    public int getCellSize() {
        return cellSize;
    }

    @StringSetter(CELL_SIZE)
    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }

    @StringGetter(NEIGHBOURHOOD_SIZE)
    public int getNeighbourhoodSize() {
        return neighbourhoodSize;
    }

    @StringSetter(NEIGHBOURHOOD_SIZE)
    public void setNeighbourhoodSize(int neighbourhoodSize) {
        this.neighbourhoodSize = neighbourhoodSize;
    }

    @StringGetter(RIDER_DEPARTURE_TIME_ADJUSTMENT)
    public double getRiderDepartureTimeAdjustment() {
        return riderDepartureTimeAdjustment;
    }

    @StringSetter(RIDER_DEPARTURE_TIME_ADJUSTMENT)
    public void setRiderDepartureTimeAdjustment(double riderDepartureTimeAdjustment) {
        this.riderDepartureTimeAdjustment = riderDepartureTimeAdjustment;
    }

    @StringGetter(TIME_SEGMENT_LENGTH)
    public int getTimeSegmentLength() {
        return timeSegmentLength;
    }

    @StringSetter(TIME_SEGMENT_LENGTH)
    public void setTimeSegmentLength(int timeSegmentLength) {
        this.timeSegmentLength = timeSegmentLength;
    }

    @StringGetter(MAX_DETOUR_FACTOR_CONSTANT)
    public double getMaxDetourFactorConstant() {
        return maxDetourFactorConstant;
    }

    @StringSetter(MAX_DETOUR_FACTOR_CONSTANT)
    public void setMaxDetourFactorConstant(double maxDetourFactorConstant) {
        this.maxDetourFactorConstant = maxDetourFactorConstant;
    }

    @StringGetter(MAX_DETOUR_FACTOR_SLOPE)
    public double getMaxDetourFactorSlope() {
        return maxDetourFactorSlope;
    }

    @StringSetter(MAX_DETOUR_FACTOR_SLOPE)
    public void setMaxDetourFactorSlope(double maxDetourFactorSlope) {
        this.maxDetourFactorSlope = maxDetourFactorSlope;
    }

    @StringGetter(PICKUP_WAITING_TIME)
    public double getPickupWaitingTime() {
        return pickupWaitingTime;
    }

    @StringSetter(PICKUP_WAITING_TIME)
    public void setPickupWaitingTime(double pickupWaitingTime) {
        this.pickupWaitingTime = pickupWaitingTime;
    }

    @StringGetter(DRIVER_PROFIT_PER_KM)
    public double getDriverProfitPerKm() {
        return driverProfitPerKm;
    }

    @StringSetter(DRIVER_PROFIT_PER_KM)
    public void setDriverProfitPerKm(double driverProfitPerKm) {
        this.driverProfitPerKm = driverProfitPerKm;
    }

    @StringGetter(RIDER_FARE_PER_KM)
    public double getRiderFarePerKm() {
        return riderFarePerKm;
    }

    @StringSetter(RIDER_FARE_PER_KM)
    public void setRiderFarePerKm(double riderFarePerKm) {
        this.riderFarePerKm = riderFarePerKm;
    }

    @StringGetter(MOBILITY_GUARANTEE)
    public boolean getMobilityGuarantee() {
        return mobilityGuarantee;
    }

    @StringSetter(MOBILITY_GUARANTEE)
    public void setMobilityGuarantee(boolean mobilityGuarantee) {
        this.mobilityGuarantee = mobilityGuarantee;
    }

    public String[] getSubtourModeChoiceModes() {
        return subtourModeChoiceModes;
    }

    @StringGetter(SUBTOUR_MODE_CHOICE_MODES)
    public String getSubtourModeChoiceModesString() {
        return toString(subtourModeChoiceModes);
    }

    public void setSubtourModeChoiceModes(String[] subtourModeChoiceModes) {
        this.subtourModeChoiceModes = subtourModeChoiceModes;
    }

    @StringSetter(SUBTOUR_MODE_CHOICE_MODES)
    public void setSubtourModeChoiceModesString(String subtourModeChoiceModes) {
        this.subtourModeChoiceModes = toArray(subtourModeChoiceModes);
    }

    public String[] getSubtourModeChoiceChainBasedModes() {
        return subtourModeChoiceChainBasedModes;
    }

    @StringGetter(SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES)
    public String getSubtourModeChoiceChainBasedModesString() {
        return toString(subtourModeChoiceChainBasedModes);
    }

    public void setSubtourModeChoiceChainBasedModes(String[] subtourModeChoiceChainBasedModes) {
        this.subtourModeChoiceChainBasedModes = subtourModeChoiceChainBasedModes;
    }

    @StringSetter(SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES)
    public void setSubtourModeChoiceChainBasedModesString(String subtourModeChoiceChainBasedModes) {
        this.subtourModeChoiceChainBasedModes = toArray(subtourModeChoiceChainBasedModes);
    }

    /** copied from SubtourModeChoiceConfigGroup */
    private static String toString(final String[] modes) {
        StringBuilder b = new StringBuilder();
        if (modes.length > 0)
            b.append(modes[0]);
        for (int i = 1; i < modes.length; i++) {
            b.append(',');
            b.append(modes[i]);
        }
        return b.toString();
    }

    /** copied from SubtourModeChoiceConfigGroup */
    private static String[] toArray(final String modes) {
        String[] parts = StringUtils.explode(modes, ',');
        for (int i = 0, n = parts.length; i < n; i++) {
            parts[i] = parts[i].trim().intern();
        }
        return parts;
    }
}
