package at.ac.ait.matsim.drs.run;

import java.util.Map;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrsConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "drs";

    public static final String MAX_MATCHING_DISTANCE_METERS = "maxMatchingDistanceMeters";
    private int maxMatchingDistanceMeters = 2000;

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

    public static final String CAR_AND_DRS_DAILY_MONETARY_CONSTANT = "carAndDrsDailyMonetaryConstant";
    private double carAndDrsDailyMonetaryConstant = 0;

    public static final String MIN_DRIVER_LEG_METERS = "minDriverLegMeters";
    private int minDriverLegMeters = 10;

    public static final String MIN_RIDER_LEG_METERS = "minRiderLegMeters";
    private int minRiderLegMeters = 10;

    public DrsConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(MAX_MATCHING_DISTANCE_METERS,
                "Maximum euclidean distance between requests to be considered by the matching algorithm, "
                        + "i.e. if both the distance between two requests' origin and destination location are closer than the given value they are potential matches. "
                        + "The default value is good for urban areas. "
                        + "For large areas with sparsely distributed population and low drs share, you may consider using a larger value.");
        map.put(MAX_POSSIBLE_CANDIDATES,
                "Limits the number of possible riders requests considered for a driver during the matching process. "
                        + "Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results. "
                        + "To turn off this feature specify a sufficiently big number (not recommended). (inspired by taxi contrib)");
        map.put(RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS,
                "The amount of time the riders are willing to adjust their departure times. "
                        + "During the matching process, the arrival of driver to pick-up point is checked whether it is within "
                        + "the rider departure time +- the riderDepartureTimeAdjustment.");
        map.put(TIME_SEGMENT_LENGTH_SECONDS,
                "The duration of the time segments used in time segment registers of riders requests. Must be larger than "
                        + RIDER_DEPARTURE_TIME_ADJUSTMENT_SECONDS
                        + ". To avoid scenarios where a driver and a rider departure time are close, but cross a segment boundary, "
                        + "candidate requests are taken not only from the current segment but also from the one before and after.");
        map.put(PICKUP_WAITING_SECONDS,
                "The amount of time the driver is expected to wait until the rider enters the vehicle.");
        map.put(DRIVER_PROFIT_PER_KM,
                "The amount of money per kilometre the driver gains for a rider (typically positive)");
        map.put(CAR_AND_DRS_DAILY_MONETARY_CONSTANT,
                "Daily price for car usage including when using the private car as drsDriver. "
                        + "If specified here do not additionaly specify it in planCalcScore.scoringParameters.modeParams.dailyMonetaryConstant - "
                        + "otherwise it will be counted twice (typically negative)");
        map.put(MIN_DRIVER_LEG_METERS,
                "minimum length of legs (routed with the drsDriver mode) to be considered for the drs driver mode. 0 means no minimum.");
        map.put(MIN_RIDER_LEG_METERS,
                "minimum length of legs (routed with the drsDriver mode) to be considered for the drs ride mode. 0 means no minimum.");
        return map;
    }

    @StringGetter(MAX_MATCHING_DISTANCE_METERS)
    public int getMaxMatchingDistanceMeters() {
        return maxMatchingDistanceMeters;
    }

    @StringSetter(MAX_MATCHING_DISTANCE_METERS)
    public void setMaxMatchingDistanceMeters(int maxMatchingDistanceMeters) {
        this.maxMatchingDistanceMeters = maxMatchingDistanceMeters;
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

    @StringGetter(CAR_AND_DRS_DAILY_MONETARY_CONSTANT)
    public double getCarAndDrsDailyMonetaryConstant() {
        return carAndDrsDailyMonetaryConstant;
    }

    @StringSetter(CAR_AND_DRS_DAILY_MONETARY_CONSTANT)
    public void setCarAndDrsDailyMonetaryConstant(double carAndDrsDailyMonetaryConstant) {
        this.carAndDrsDailyMonetaryConstant = carAndDrsDailyMonetaryConstant;
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

}
