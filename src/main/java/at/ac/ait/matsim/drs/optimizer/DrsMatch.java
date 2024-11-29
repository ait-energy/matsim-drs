package at.ac.ait.matsim.drs.optimizer;

import org.matsim.api.core.v01.population.Leg;

import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;

public class DrsMatch {

    private final DrsDriverRequest driver;
    private final DrsRiderRequest rider;
    private final Leg toPickup, withCustomer, afterDropoff;
    private final Double detourFactor;

    private DrsMatch(DrsDriverRequest driver, DrsRiderRequest rider, Leg toPickup, Leg withCustomer,
            Leg afterDropoff, Double detourFactor) {
        this.driver = driver;
        this.rider = rider;
        this.toPickup = toPickup;
        this.withCustomer = withCustomer;
        this.afterDropoff = afterDropoff;
        this.detourFactor = detourFactor;
    }

    public static DrsMatch createMinimal(DrsDriverRequest driver, DrsRiderRequest rider, Leg toPickup) {
        return new DrsMatch(driver, rider, toPickup, null, null, null);
    }

    public static DrsMatch create(DrsDriverRequest driver, DrsRiderRequest rider, Leg toPickup,
            Leg withCustomer, Leg afterDropoff, Double detourFactor) {
        return new DrsMatch(driver, rider, toPickup, withCustomer, afterDropoff, detourFactor);
    }

    public DrsDriverRequest getDriver() {
        return driver;
    }

    public DrsRiderRequest getRider() {
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
