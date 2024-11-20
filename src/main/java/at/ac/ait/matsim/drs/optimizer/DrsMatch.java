package at.ac.ait.matsim.drs.optimizer;

import org.matsim.api.core.v01.population.Leg;

public class DrsMatch {

    private final DrsRequest driver, rider;
    private final Leg toPickup, withCustomer, afterDropoff;
    private final Double detourFactor;

    private DrsMatch(DrsRequest driver, DrsRequest rider, Leg toPickup, Leg withCustomer,
            Leg afterDropoff, Double detourFactor) {
        this.driver = driver;
        this.rider = rider;
        this.toPickup = toPickup;
        this.withCustomer = withCustomer;
        this.afterDropoff = afterDropoff;
        this.detourFactor = detourFactor;
    }

    public static DrsMatch createMinimal(DrsRequest driver, DrsRequest rider, Leg toPickup) {
        return new DrsMatch(driver, rider, toPickup, null, null, null);
    }

    public static DrsMatch create(DrsRequest driver, DrsRequest rider, Leg toPickup,
            Leg withCustomer, Leg afterDropoff, Double detourFactor) {
        return new DrsMatch(driver, rider, toPickup, withCustomer, afterDropoff, detourFactor);
    }

    public DrsRequest getDriver() {
        return driver;
    }

    public DrsRequest getRider() {
        return rider;
    }

    public Leg getToPickup() {
        return toPickup;
    }

    public Leg getWithCustomer() {
        return withCustomer;
    }

    public Leg getAfterDropoff() {
        return afterDropoff;
    }

    public Double getDetourFactor() {
        return detourFactor;
    }

}
