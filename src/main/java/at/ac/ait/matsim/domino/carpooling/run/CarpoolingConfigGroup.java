package at.ac.ait.matsim.domino.carpooling.run;

import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.utils.misc.StringUtils;

import at.ac.ait.matsim.domino.carpooling.replanning.SubtourModeChoiceForCarpooling;

public class CarpoolingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "carpooling";

    public static final String CELL_SIZE = "cellSize";
    private int cellSize = 4000;

    public static final String TIME_SEGMENT_LENGTH_SECONDS = "timeSegmentLengthSeconds";
    private int timeSegmentLengthSeconds = 2 * 60 * 60;

    public static final String MAX_POSSIBLE_CANDIDATES = "maxPossibleCandidates";
    private int maxPossibleCandidates = 30;

    public static final String RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS = "riderDepartureTimeAdjustmentSeconds";
    private int riderDepartureTimeAdjustmentSeconds = 15 * 60;

    public static final String PICKUP_WAITING_SECONDS = "pickupWaitingSeconds";
    public int pickupWaitingSeconds = 0;

    public static final String DRIVER_PROFIT_PER_KM = "driverProfitPerKm";
    private double driverProfitPerKm = 0;

    public static final String CAR_AND_CARPOOLING_DAILY_MONETARY_CONSTANT = "carAndCarpoolingDailyMonetaryConstant";
    private double carAndCarpoolingDailyMonetaryConstant = 0;

    public static final String SUBTOUR_MODE_CHOICE_MODES = "subtourModeChoiceModes";
    private String[] subtourModeChoiceModes = { TransportMode.car, Carpooling.DRIVER_MODE, Carpooling.RIDER_MODE,
            TransportMode.pt, TransportMode.bike, TransportMode.walk };

    public static final String SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES = "subtourModeChoiceChainBasedModes";
    private String[] subtourModeChoiceChainBasedModes = new String[] { TransportMode.car, Carpooling.DRIVER_MODE,
            TransportMode.bike };

    public static final String MIN_DRIVER_LEG_METERS = "minDriverLegMeters";
    private int minDriverLegMeters = 10;

    public static final String MIN_RIDER_LEG_METERS = "minRiderLegMeters";
    private int minRiderLegMeters = 10;

    public CarpoolingConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(CELL_SIZE,
                "The side length of square zones in meters used in zonal registers of riders requests. The default value is good for urban areas. For large areas with sparsely distributed population and low carpooling share, you may consider using a bigger cell size. On the other hand, if neighbourhoodSize is very low, a smaller cell size may work better. (inspired by taxi contrib)");
        map.put(MAX_POSSIBLE_CANDIDATES,
                "Limits the number of possible riders requests considered for a driver during the matching process. Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results. To turn off this feature specify a sufficiently big number (not recommended). (inspired by taxi contrib)");
        map.put(RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS,
                "The amount of time the riders are willing to adjust their departure times. During the matching process, the arrival of driver to pick-up point is checked whether it is within the rider departure time +- the riderDepartureTimeAdjustment.");
        map.put(TIME_SEGMENT_LENGTH_SECONDS,
                "The duration of the time segments used in time segment registers of riders requests. To avoid scenarios where a driver and a rider departure time are close but cross a segment boundary candidate requests are token not only from the current segment but also from the one before and after.");
        map.put(PICKUP_WAITING_SECONDS,
                "The amount of time the driver is expected to wait until the rider enters the vehicle.");
        map.put(DRIVER_PROFIT_PER_KM,
                "The amount of money per kilometre the driver gains for a rider (typically positive)");
        map.put(CAR_AND_CARPOOLING_DAILY_MONETARY_CONSTANT,
                "Daily price for car usage including when using the private car as carpoolingDriver. If specified here do not additionaly specify it in planCalcScore.scoringParameters.modeParams.dailyMonetaryConstant - otherwise it will be counted twice (typically negative)");
        map.put(SUBTOUR_MODE_CHOICE_MODES,
                "Defines all modes available for the '" + SubtourModeChoiceForCarpooling.STRATEGY_NAME
                        + "' strategy, including chain-based modes, separated by commas");
        map.put(SUBTOUR_MODE_CHOICE_CHAIN_BASED_MODES,
                "Defines the chain-based modes for the'" + SubtourModeChoiceForCarpooling.STRATEGY_NAME
                        + "' strategy, separated by commas");
        map.put(MIN_DRIVER_LEG_METERS,
                "minimum length of legs (routed with the carpoolingDriver mode) to be considered for the carpooling driver mode. 0 means no minimum.");
        map.put(MIN_RIDER_LEG_METERS,
                "minimum length of legs (routed with the carpoolingDriver mode) to be considered for the carpooling ride mode. 0 means no minimum.");
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

    @StringGetter(MAX_POSSIBLE_CANDIDATES)
    public int getMaxPossibleCandidates() {
        return maxPossibleCandidates;
    }

    @StringSetter(MAX_POSSIBLE_CANDIDATES)
    public void setMaxPossibleCandidates(int maxPossibleCandidates) {
        this.maxPossibleCandidates = maxPossibleCandidates;
    }

    @StringGetter(RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS)
    public int getRiderDepartureTimeAdjustmentSeconds() {
        return riderDepartureTimeAdjustmentSeconds;
    }

    @StringSetter(RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS)
    public void setRiderDepartureTimeAdjustmentSeconds(int riderDepartureTimeAdjustmentSeconds) {
        this.riderDepartureTimeAdjustmentSeconds = riderDepartureTimeAdjustmentSeconds;
    }

    @StringGetter(TIME_SEGMENT_LENGTH_SECONDS)
    public int getTimeSegmentLengthSeconds() {
        return timeSegmentLengthSeconds;
    }

    @StringSetter(TIME_SEGMENT_LENGTH_SECONDS)
    public void setTimeSegmentLengthSeconds(int timeSegmentLengthSeconds) {
        this.timeSegmentLengthSeconds = timeSegmentLengthSeconds;
    }

    @StringGetter(PICKUP_WAITING_SECONDS)
    public int getPickupWaitingSeconds() {
        return pickupWaitingSeconds;
    }

    @StringSetter(PICKUP_WAITING_SECONDS)
    public void setPickupWaitingSeconds(int pickupWaitingSeconds) {
        this.pickupWaitingSeconds = pickupWaitingSeconds;
    }

    @StringGetter(DRIVER_PROFIT_PER_KM)
    public double getDriverProfitPerKm() {
        return driverProfitPerKm;
    }

    @StringSetter(DRIVER_PROFIT_PER_KM)
    public void setDriverProfitPerKm(double driverProfitPerKm) {
        this.driverProfitPerKm = driverProfitPerKm;
    }

    @StringGetter(CAR_AND_CARPOOLING_DAILY_MONETARY_CONSTANT)
    public double getCarAndCarpoolingDailyMonetaryConstant() {
        return carAndCarpoolingDailyMonetaryConstant;
    }

    @StringSetter(CAR_AND_CARPOOLING_DAILY_MONETARY_CONSTANT)
    public void setCarAndCarpoolingDailyMonetaryConstant(double carAndCarpoolingDailyMonetaryConstant) {
        this.carAndCarpoolingDailyMonetaryConstant = carAndCarpoolingDailyMonetaryConstant;
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

    @StringGetter(MIN_DRIVER_LEG_METERS)
    public int getMinDriverLegMeters() {
        return minDriverLegMeters;
    }

    @StringSetter(MIN_DRIVER_LEG_METERS)
    public void setMinDriverLegMeters(int minDriverLegMeters) {
        this.minDriverLegMeters = minDriverLegMeters;
    }

    @StringGetter(MIN_RIDER_LEG_METERS)
    public int getMinRiderLegMeters() {
        return minRiderLegMeters;
    }

    @StringSetter(MIN_RIDER_LEG_METERS)
    public void setMinRiderLegMeters(int minRiderLegMeters) {
        this.minRiderLegMeters = minRiderLegMeters;
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
