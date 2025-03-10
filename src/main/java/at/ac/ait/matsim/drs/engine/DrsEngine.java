package at.ac.ait.matsim.drs.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
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

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.Drs.ActivityType;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;
import jakarta.inject.Inject;

/**
 * Engine to handle drs agents during QSIM.
 *
 * Heavily inspired by ActivityEngineDefaultImpl and
 * org.matsim.contrib.dvrp.passenger.InternalPassengerHandling
 *
 * Note on multi-threading: this class is bound as Singleton in
 * DrsEngineQSimModule
 */
public class DrsEngine implements MobsimEngine, ActivityHandler, DepartureHandler {
    public static final String COMPONENT_NAME = "drsEngine";
    private static final Logger LOGGER = LogManager.getLogger();
    private final DrsConfigGroup drsConfig;
    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    private final Map<Id<Person>, Id<Link>> waitingRiders = new ConcurrentHashMap<>();
    private final List<PickupEntry> waitingDrivers = new CopyOnWriteArrayList<>();

    private static record PickupEntry(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId,
            double latestPickupTime) {
    };

    @Inject
    public DrsEngine(DrsConfigGroup drsConfig, EventsManager eventsManager) {
        this.drsConfig = drsConfig;
        this.eventsManager = eventsManager;
        LOGGER.info("Constructed new DrsEngine.");
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
    }

    @Override
    public void onPrepareSim() {
    }

    @Override
    public void doSimStep(double time) {
        retryPreviouslyUnsuccessfulPickups(time);
    }

    private void retryPreviouslyUnsuccessfulPickups(double time) {
        if (waitingDrivers.isEmpty()) {
            return;
        }

        waitingDrivers.removeIf(waitingDriver -> {
            boolean driverStillWaiting = handlePickup(waitingDriver, time);
            return !driverStillWaiting;
        });
    }

    @Override
    public void afterSim() {
        double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
        if (!waitingDrivers.isEmpty()) {
            LOGGER.warn("{} drs driver(s) stuck (still waiting):", waitingDrivers.size());
            waitingDrivers.forEach(d -> {
                LOGGER.warn("- driver {} on link {}", d.driver.getId(), d.linkId);
                eventsManager.processEvent(
                        new PersonStuckEvent(now, d.driver.getId(), d.linkId, Drs.DRIVER_MODE));
            });
            waitingDrivers.clear();
        }
        if (!waitingRiders.isEmpty()) {
            LOGGER.warn("{} drs rider(s) stuck (still waiting):", waitingRiders.size());
            waitingRiders.keySet().forEach(d -> {
                LOGGER.warn("- rider {} on link {}", d, waitingRiders.get(d));
                eventsManager.processEvent(
                        new PersonStuckEvent(now, d, null, Drs.RIDER_MODE));
            });
            waitingRiders.clear();
        }
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
            // agent.getMode(), agent.getId(), Time.writeTime(now), linkId);
            Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
            String driverId = DrsUtil.getAssignedDriver(currentLeg);
            if (driverId != null) {
                LOGGER.debug("{} is waiting to be picked up by {} on link {} @ {}.",
                        agent.getId(), driverId, linkId, Time.writeTime(now));
                waitingRiders.put(agent.getId(), linkId);
                return true;
            }
            LOGGER.warn("Agent {} is teleported using {}. This should not happen anymore!", agent.getId(),
                    Drs.RIDER_MODE);
        }
        return false;
    }

    /**
     * Takes care of special handling required for drivers picking up riders.
     *
     * @return true if the activity has been handled and should not be handled by
     *         any other ActivityHandler
     */
    @Override
    public boolean handleActivity(MobsimAgent agent) {
        Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
        double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
        // LOGGER.debug("handleActivity {} for {} @ {} ", agent.getId(),
        // ((Activity) ((PlanAgent) agent).getCurrentPlanElement()).getType(),
        // Time.writeTime(now));
        if (agent instanceof PlanAgent && agent instanceof MobsimDriverAgent) {
            if (act.getType().equals(Drs.DRIVER_INTERACTION)) {
                ActivityType type = DrsUtil.getActivityType(act);
                Id<Person> riderId = DrsUtil.getRiderId(act);
                Id<Link> linkId = agent.getCurrentLinkId();
                MobsimPassengerAgent rider = (MobsimPassengerAgent) internalInterface.getMobsim().getAgents()
                        .get(riderId);
                // LOGGER.debug("handleActivity {} for {} @ {} on link {}", act.getType(),
                // agent.getId(), Time.writeTime(now), linkId);
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
                        PickupEntry pickup = new PickupEntry((MobsimDriverAgent) agent, rider, linkId,
                                agent.getActivityEndTime());
                        boolean driverStillWaiting = handlePickup(pickup, now);
                        if (driverStillWaiting) {
                            waitingDrivers.add(pickup);
                        }
                        // return true so that the ActivityEngineDefaultImpl never schedules
                        // our activity and we can quit it early ourselves
                        return true;
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
     * Sets the drs status of legs, which is relevant for the vkt analysis.
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
        DrsUtil.setDrsStatus(legWithRider, Drs.VALUE_STATUS_DRS);

        int legBeforeRiderIndex = dropoffIndex - 3;
        if (0 <= legBeforeRiderIndex && legBeforeRiderIndex < driverPlanElements.size()) {
            Leg legBeforeRider = (Leg) driverPlanElements.get(legBeforeRiderIndex);
            DrsUtil.setDrsStatus(legBeforeRider, Drs.VALUE_STATUS_BEFORE_AFTER);
        }

        int legAfterRiderIndex = dropoffIndex + 1;
        if (0 <= legAfterRiderIndex && legAfterRiderIndex < driverPlanElements.size()) {
            Leg legAfterRider = (Leg) driverPlanElements.get(legAfterRiderIndex);
            DrsUtil.setDrsStatus(legAfterRider, Drs.VALUE_STATUS_BEFORE_AFTER);
        }

        Leg planElement = (Leg) ((PlanAgent) rider).getCurrentPlanElement();
        int legIndex = ((PlanAgent) rider).getCurrentPlan().getPlanElements().indexOf(planElement);
        List<PlanElement> riderPlanElements = PopulationUtils
                .findPerson(rider.getId(), internalInterface.getMobsim().getScenario())
                .getSelectedPlan()
                .getPlanElements();
        Leg leg = (Leg) riderPlanElements.get(legIndex);
        DrsUtil.setDrsStatus(leg, Drs.VALUE_STATUS_DRS);
        double distance = legWithRider.getRoute().getDistance();

        if (drsConfig.getDriverProfitPerKm() != 0) {
            eventsManager.processEvent(new PersonMoneyEvent(
                    now,
                    driver.getId(),
                    drsConfig.getDriverProfitPerKm() * distance / 1000d,
                    Drs.DRIVER_MODE + " profit",
                    rider.getId().toString(),
                    null));
        }

        driver.getVehicle().removePassenger(rider);
        rider.setVehicle(null);
        eventsManager.processEvent(new PersonLeavesVehicleEvent(now, rider.getId(), driver.getVehicle().getId()));
        rider.notifyArrivalOnLinkByNonNetworkMode(rider.getDestinationLinkId());
        rider.endLegAndComputeNextState(now);
        try {
            internalInterface.arrangeNextAgentState(rider);
        } catch (Exception e) {
            LOGGER.error("Failed to arrangeNextAgentState for rider agent {} after dropoff by {}", rider.getId(),
                    driver.getId());
            throw e;
        }
    }

    /**
     * Either pick up a rider (i.e. put the rider into the driver's simulated
     * vehicle), wait for the rider to arrive, or continue driving
     *
     * @return true if the rider is not there yet and the driver is still willing
     *         to wait
     */
    private boolean handlePickup(PickupEntry entry, double now) {
        MobsimDriverAgent driver = entry.driver;
        MobsimPassengerAgent rider = entry.rider;
        Id<Link> linkId = entry.linkId;

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
                        "{} {} could not pick up {} from link {} at end of pickup activity (code {}). Driving on.",
                        Time.writeTime(now), driver.getId(), rider.getId(), linkId, errorCode);
                eventsManager.processEvent(new DrsFailedPickupEvent(now, linkId, driver.getId(), rider.getId(),
                        driver.getVehicle().getId()));
                driver.endActivityAndComputeNextState(now);
                internalInterface.arrangeNextAgentState(driver);
                return false;
            }
            LOGGER.debug(
                    "{} {} could not pick up {} from link {} ({}). Waiting.",
                    Time.writeTime(now), driver.getId(), rider.getId(), linkId, errorCode);
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

        // end the interaction activity early due to successful pickup
        driver.endActivityAndComputeNextState(now);
        try {
            internalInterface.arrangeNextAgentState(driver);
        } catch (Exception e) {
            LOGGER.error("Failed to arrangeNextAgentState for driver agent {} after pickup of {}", driver.getId(),
                    rider.getId());
            throw e;
        }
        return false;
    }

    @Override
    public void rescheduleActivityEnd(MobsimAgent agent) {
    }

}
