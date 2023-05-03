package at.ac.ait.matsim.domino.carpooling.request;

import org.matsim.api.core.v01.population.Leg;

public class CarpoolingMatch {

    private final CarpoolingRequest driver, rider;
    private final Leg toPickup, withCustomer, afterDropoff;
    private final Double detourFactor;

    private CarpoolingMatch(CarpoolingRequest driver, CarpoolingRequest rider, Leg toPickup, Leg withCustomer,
            Leg afterDropoff, Double detourFactor) {
        this.driver = driver;
        this.rider = rider;
        this.toPickup = toPickup;
        this.withCustomer = withCustomer;
        this.afterDropoff = afterDropoff;
        this.detourFactor = detourFactor;
    }

    public static CarpoolingMatch createMinimal(CarpoolingRequest driver, CarpoolingRequest rider, Leg toPickup) {
        return new CarpoolingMatch(driver, rider, toPickup, null, null, null);
    }

    public static CarpoolingMatch create(CarpoolingRequest driver, CarpoolingRequest rider, Leg toPickup,
            Leg withCustomer, Leg afterDropoff, Double detourFactor) {
        return new CarpoolingMatch(driver, rider, toPickup, withCustomer, afterDropoff, detourFactor);
    }

    public CarpoolingRequest getDriver() {
        return driver;
    }

    public CarpoolingRequest getRider() {
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
