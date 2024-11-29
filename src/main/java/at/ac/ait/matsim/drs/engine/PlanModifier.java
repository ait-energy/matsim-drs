package at.ac.ait.matsim.drs.engine;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.optimizer.DrsMatch;
import at.ac.ait.matsim.drs.optimizer.DrsRequest;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class PlanModifier {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DrsConfigGroup drsConfig;
    private final PopulationFactory populationFactory;

    public PlanModifier(DrsConfigGroup drsConfig, PopulationFactory populationFactory) {
        this.drsConfig = drsConfig;
        this.populationFactory = populationFactory;
    }

    public void modifyPlans(DrsMatch match) {
        DrsUtil.setAssignedDriver(match.getRider().getDrsLeg(), match.getDriver().getPerson().getId().toString());
        double pickupTime = match.getDriver().getDepartureTime() + match.getToPickup().getTravelTime().seconds();
        addNewActivitiesToDriverPlan(match, pickupTime);
        addRiderRoute(match.getRider());
        adjustRiderDepartureTime(match.getRider(), pickupTime);
    }

    private void addNewActivitiesToDriverPlan(DrsMatch match, double pickupTime) {
        Activity pickup = populationFactory.createActivityFromLinkId(Drs.DRIVER_INTERACTION,
                match.getRider().getFromLink().getId());
        pickup.setEndTime(pickupTime + drsConfig.getPickupWaitingSeconds());
        DrsUtil.setActivityType(pickup, Drs.ActivityType.pickup);
        DrsUtil.setRiderId(pickup, match.getRider().getPerson().getId());

        double dropoffTime = pickupTime + match.getWithCustomer().getTravelTime().seconds();
        Activity dropoff = populationFactory.createActivityFromLinkId(Drs.DRIVER_INTERACTION,
                match.getRider().getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        DrsUtil.setActivityType(dropoff, Drs.ActivityType.dropoff);
        DrsUtil.setRiderId(dropoff, match.getRider().getPerson().getId());

        List<PlanElement> newTrip = Arrays.asList(match.getToPickup(), pickup, match.getWithCustomer(), dropoff,
                match.getAfterDropoff());
        DrsUtil.setRoutingModeToDriver(newTrip);
        Activity startActivity = match.getDriver().getTrip().getOriginActivity();
        Activity endActivity = match.getDriver().getTrip().getDestinationActivity();
        List<PlanElement> planElements = match.getDriver().getPerson().getSelectedPlan().getPlanElements();
        LOGGER.debug("Before matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
        TripRouter.insertTrip(planElements, startActivity, newTrip, endActivity);
        LOGGER.debug("After matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
    }

    /**
     * For rider legs it is important to set the routing mode and a generic route
     * so that PersonPrepareForSim does not reroute our legs (and thereby discards
     * our leg attributes where we store if the person found a match or not)
     */
    private void addRiderRoute(DrsRequest riderRequest) {
        Route genericRoute = new GenericRouteImpl(riderRequest.getFromLink().getId(),
                riderRequest.getToLink().getId());
        genericRoute.setDistance(riderRequest.getNetworkRouteDistance());
        genericRoute.setTravelTime(riderRequest.getNetworkRouteTravelTime().seconds());

        Leg leg = riderRequest.getDrsLeg();
        leg.setRoute(genericRoute);
        TripStructureUtils.setRoutingMode(leg, Drs.RIDER_MODE);
    }

    /**
     * Note: only adjusts one activity!
     * It may happen that later activities start before the end of the adjusted
     * activity.
     *
     * @param riderRequest
     * @param pickupTime
     */
    static void adjustRiderDepartureTime(DrsRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    Activity act = (Activity) planElement;
                    if (act.getEndTime().isUndefined()) {
                        continue;
                    }
                    int endTime = (int) act.getEndTime().seconds();
                    if (endTime == (int) riderRequest.getDepartureTime()) {
                        DrsUtil.setActivityOriginalDepartureTime((Activity) planElement,
                                riderRequest.getDepartureTime());
                        LOGGER.debug("Before matching " + riderRequest.getPerson().getId().toString()
                                + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                        ((Activity) planElement).setEndTime(pickupTime);
                        LOGGER.debug("After matching " + riderRequest.getPerson().getId().toString()
                                + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                        break;
                    }
                }
            }
        }
    }

}
