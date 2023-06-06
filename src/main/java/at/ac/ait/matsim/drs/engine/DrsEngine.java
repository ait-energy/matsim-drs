package at.ac.ait.matsim.drs.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.Time;

import at.ac.ait.matsim.drs.events.DrsPickupEvent;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.Drs.ActivityType;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Heavily inspired by ActivityEngineDefaultImpl and
 * org.matsim.contrib.dvrp.passenger.InternalPassengerHandling
 */
public class DrsEngine implements MobsimEngine, ActivityHandler, DepartureHandler {
    public static final String COMPONENT_NAME = "carpoolingEngine";
    private static final Logger LOGGER = LogManager.getLogger();
    private final DrsConfigGroup cfgGroup;
    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    private final Map<Id<Person>, Id<Link>> waitingRiders = new ConcurrentHashMap<>();

    @Inject
    public DrsEngine(Scenario scenario, EventsManager eventsManager) {
        LOGGER.info("Constructing new CarpoolingEngine");
        this.cfgGroup = Drs.addOrGetConfigGroup(scenario);
        this.eventsManager = eventsManager;
    }

    private static class PickupEntry {
        PickupEntry(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId, double latestPickupTime) {
            this.driver = driver;
            this.rider = rider;
            this.linkId = linkId;
            this.latestPickupTime = latestPickupTime;
        }

        private final MobsimDriverAgent driver;
        private final MobsimPassengerAgent rider;
        private final Id<Link> linkId;
        private final double latestPickupTime;
    }

    private final Queue<PickupEntry> pickupQueue = new PriorityBlockingQueue<>(500, (e0, e1) -> {
        int cmp = Double.compare(e0.latestPickupTime, e1.latestPickupTime);
        if (cmp == 0) {
            return e1.driver.getId().compareTo(e0.driver.getId());
        }
        return cmp;
    });

    @Override
    public void onPrepareSim() {
    }

    /**
     * retry previously unsuccessful pickups at latest pickup time
     */
    @Override
    public void doSimStep(double time) {
        while (pickupQueue.peek() != null) {
            if (pickupQueue.peek().latestPickupTime <= time) {
                PickupEntry pickup = pickupQueue.poll();
                if (!handlePickup(pickup.driver, pickup.rider, pickup.linkId, time)) {
                    pickup.driver.endActivityAndComputeNextState(time);
                    internalInterface.arrangeNextAgentState(pickup.driver);
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void afterSim() {
        double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
        if (!pickupQueue.isEmpty()) {
            LOGGER.warn("{} carpooling drivers were still waiting for their pickup and are stuck.", pickupQueue.size());
            pickupQueue.forEach(d -> eventsManager.processEvent(
                    new PersonStuckEvent(now, d.driver.getId(), d.linkId, Drs.DRIVER_MODE)));
            pickupQueue.clear();
        }
        if (!waitingRiders.isEmpty()) {
            LOGGER.warn("{} carpooling riders were still waiting to be picked up and are stuck.", waitingRiders.size());
            waitingRiders.keySet().forEach(d -> eventsManager.processEvent(
                    new PersonStuckEvent(now, d, null, Drs.RIDER_MODE)));
            waitingRiders.clear();
        }
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
    }

    /**
     * Register riders that are ready to be picked up.
     * 
     * @return true for riders so that they actually wait for the driver instead of
     *         being handled by a different engine (e.g. teleportation)
     */
    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
        if (agent.getMode().equals(Drs.RIDER_MODE)) {
            // LOGGER.debug("handleDeparture {} for agent {} @ {} on link {}",
            // agent.getMode(), agent.getId(), now, linkId);
            Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
            if (Objects.equals(DrsUtil.getRequestStatus(currentLeg), Drs.REQUEST_STATUS_MATCHED)) {
                LOGGER.debug("{} {} is waiting to be picked up on link {}.",
                        Time.writeTime(now), agent.getId(), linkId);
                waitingRiders.put(agent.getId(), linkId);
                return true;
            }
            eventsManager.processEvent(new PersonMoneyEvent(
                    now,
                    agent.getId(),
                    cfgGroup.getRiderMobilityGuaranteeMonetaryConstant(),
                    Drs.RIDER_MODE + " mobility guarantee",
                    null,
                    null));
        }
        return false;
    }

    /**
     * Takes care of special handling required for drivers picking up riders.
     * For all other activities (and also as final step for our drivers) the
     * delegate default handler is called.
     */
    @Override
    public boolean handleActivity(MobsimAgent agent) {
        if (agent instanceof PlanAgent && agent instanceof MobsimDriverAgent) {
            Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
            if (act.getType().equals(Drs.DRIVER_INTERACTION)) {
                double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
                ActivityType type = DrsUtil.getActivityType(act);
                Id<Person> riderId = DrsUtil.getRiderId(act);
                Id<Link> linkId = agent.getCurrentLinkId();
                MobsimPassengerAgent rider = (MobsimPassengerAgent) internalInterface.getMobsim().getAgents()
                        .get(riderId);
                // LOGGER.debug("handleActivity {} for {} @ {} on link {}", act.getType(),
                // agent.getId(), now, linkId);
                if (rider == null) {
                    LOGGER.warn(
                            "{} {} wanted to {} {} on link {}, but it is no longer an active qsim agent",
                            Time.writeTime(now), agent.getId(), type, riderId, linkId);
                    return false;
                }
                switch (Objects.requireNonNull(type)) {
                    case dropoff:
                        int dropOffIndex = ((PlanAgent) agent).getCurrentPlan().getPlanElements().indexOf(act);
                        handleDropoff((MobsimDriverAgent) agent, rider, linkId, now, dropOffIndex);
                        break;
                    case pickup:
                        return handlePickup((MobsimDriverAgent) agent, rider, linkId, now);
                    default:
                        throw new IllegalArgumentException("unknown activity " + type);
                }
            }
        }
        return false;
    }

    /**
     * Drop off rider on a link (if it is actually in the vehicle) and pay the
     * driver (MoneyEvent).
     * Sets the carpooling status of legs, which is relevant for the vkt analysis.
     */
    private void handleDropoff(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId,
            double now, int dropoffIndex) {
        if (!driver.getVehicle().getPassengers().contains(rider)) {
            LOGGER.debug("{} {} wanted to drop off {} on link {}, but it never entered the vehicle",
                    Time.writeTime(now), driver.getId(), rider.getId(), linkId);
            return;
        }
        LOGGER.debug("{} {} drops off {} on link {}", Time.writeTime(now), driver.getId(), rider.getId(), linkId);

        List<PlanElement> driverPlanElements = PopulationUtils
                .findPerson(driver.getId(), internalInterface.getMobsim().getScenario()).getSelectedPlan()
                .getPlanElements();
        Leg legWithRider = (Leg) driverPlanElements.get(dropoffIndex - 1);
        DrsUtil.setCarpoolingStatus(legWithRider, Drs.VALUE_STATUS_CARPOOLING);

        int legBeforeRiderIndex = dropoffIndex - 3;
        if (0 <= legBeforeRiderIndex && legBeforeRiderIndex < driverPlanElements.size()) {
            Leg legBeforeRider = (Leg) driverPlanElements.get(legBeforeRiderIndex);
            DrsUtil.setCarpoolingStatus(legBeforeRider, Drs.VALUE_STATUS_BEFORE_AFTER);
        }

        int legAfterRiderIndex = dropoffIndex + 1;
        if (0 <= legAfterRiderIndex && legAfterRiderIndex < driverPlanElements.size()) {
            Leg legAfterRider = (Leg) driverPlanElements.get(legAfterRiderIndex);
            DrsUtil.setCarpoolingStatus(legAfterRider, Drs.VALUE_STATUS_BEFORE_AFTER);
        }

        Leg planElement = (Leg) ((PlanAgent) rider).getCurrentPlanElement();
        int legIndex = ((PlanAgent) rider).getCurrentPlan().getPlanElements().indexOf(planElement);
        List<PlanElement> riderPlanElements = PopulationUtils
                .findPerson(rider.getId(), internalInterface.getMobsim().getScenario())
                .getSelectedPlan()
                .getPlanElements();
        Leg leg = (Leg) riderPlanElements.get(legIndex);
        DrsUtil.setCarpoolingStatus(leg, Drs.VALUE_STATUS_CARPOOLING);
        double distance = legWithRider.getRoute().getDistance();

        if (cfgGroup.getDriverProfitPerKm() != 0) {
            eventsManager.processEvent(new PersonMoneyEvent(
                    now,
                    driver.getId(),
                    cfgGroup.getDriverProfitPerKm() * distance / 1000d,
                    Drs.DRIVER_MODE + " profit",
                    rider.getId().toString(),
                    null));
        }

        driver.getVehicle().removePassenger(rider);
        rider.setVehicle(null);
        eventsManager.processEvent(new PersonLeavesVehicleEvent(now, rider.getId(), driver.getVehicle().getId()));
        rider.notifyArrivalOnLinkByNonNetworkMode(rider.getDestinationLinkId());
        rider.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(rider);
    }

    /**
     * Pick up a rider, i.e. put the rider into the driver's simulated vehicle.
     * If the rider is not there yet let the driver wait until the end of the pickup
     * activity
     * 
     * @return if the driver handling is finished (note: only returns true if it
     *         needs to wait more time for the rider to show up)
     */
    private boolean handlePickup(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId, double now) {
        // agents that drive a vehicle before being passenger actually have a vehicle
        // assigned. therefore putting these agents into a different vehicle seems fine.
        boolean riderInVehicle = false; // rider.getVehicle() != null;
        boolean riderOnOtherLink = !rider.getCurrentLinkId().equals(linkId);
        boolean riderNotWaitingForThisPickup = !waitingRiders.containsKey(rider.getId())
                || !waitingRiders.get(rider.getId()).equals(linkId);
        String errorCode = (riderInVehicle ? "V" : "") + (riderOnOtherLink ? "L" : "")
                + (riderNotWaitingForThisPickup ? "W" : "");

        if (riderInVehicle || riderOnOtherLink || riderNotWaitingForThisPickup) {
            if (driver.getActivityEndTime() <= now) {
                LOGGER.warn(
                        "{} {} wanted to pick up {} from link {}, but it is still not ready at the end of the pickup activity (code {}). Driving on.",
                        Time.writeTime(now), driver.getId(), rider.getId(), linkId, errorCode);
                return false;
            }
            LOGGER.warn(
                    "{} {} wanted to pick up {} from link {}, but it is not ready (code {}). Driver waits until the end of the pickup activity.",
                    Time.writeTime(now), driver.getId(), rider.getId(), linkId, errorCode);
            pickupQueue.add(new PickupEntry(driver, rider, linkId, driver.getActivityEndTime()));
            return true;
        }

        LOGGER.debug("{} {} picks up {} from link {} and starts driving.",
                Time.writeTime(now), driver.getId(), rider.getId(), linkId);
        driver.getVehicle().addPassenger(rider);
        rider.setVehicle(driver.getVehicle());
        internalInterface.unregisterAdditionalAgentOnLink(rider.getId(), linkId);
        eventsManager.processEvent(
                new DrsPickupEvent(now, linkId, driver.getId(), rider.getId(), driver.getVehicle().getId()));
        eventsManager.processEvent(new PersonEntersVehicleEvent(now, rider.getId(), driver.getVehicle().getId()));
        waitingRiders.remove(rider.getId());
        return false;
    }

    @Override
    public void rescheduleActivityEnd(MobsimAgent agent) {
    }

}
