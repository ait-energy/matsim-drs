package at.ac.ait.matsim.domino.carpooling.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
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

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling.ActivityType;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.core.population.PopulationUtils;

/**
 * Heavily inspired by
 * org.matsim.contrib.dvrp.passenger.InternalPassengerHandling
 */
public class CarpoolingEngine implements MobsimEngine, ActivityHandler, DepartureHandler {
    public static final String COMPONENT_NAME = "carpoolingEngine";
    private static final Logger LOGGER = LogManager.getLogger();
    private final CarpoolingConfigGroup cfgGroup;
    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    private final Map<Id<Person>, Id<Link>> waitingRiders = new HashMap<>();

    @Inject
    public CarpoolingEngine(Scenario scenario, EventsManager eventsManager) {
        LOGGER.info("Constructing new CarpoolingEngine");
        this.cfgGroup = Carpooling.addOrGetConfigGroup(scenario);
        this.eventsManager = eventsManager;
    }

    @Override
    public void doSimStep(double time) {
    }

    @Override
    public void onPrepareSim() {
        LOGGER.info("onPrepareSim");
    }

    @Override
    public void afterSim() {
        LOGGER.info("afterSim");
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
    }

    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> id) {
        if (agent.getMode().equals(Carpooling.RIDER_MODE)) {
            LOGGER.debug("handleDeparture of {} leg for agent {} on link {}", agent.getMode(), agent.getId(), id);
            Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
            if (Objects.equals(CarpoolingUtil.getRequestStatus(currentLeg), "matched")) {
                LOGGER.debug("{} is waiting to be picked up.", agent.getId());
                waitingRiders.put(agent.getId(), id);
                return true;
            }
        }
        return false;
    }

    /**
     * Will always return false so that the default activity handler properly
     * handles the activities of the driver.
     * Not sure if this is a clean approach, but duplicating code from
     * ActivityEngineDefaultImpl.handleActivity seems not so great either.
     */
    @Override
    public boolean handleActivity(MobsimAgent agent) {
        if (agent instanceof PlanAgent && agent instanceof MobsimDriverAgent) {
            Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
            if (act.getType().equals(Carpooling.DRIVER_INTERACTION)) {
                LOGGER.debug("handleActivity {} for {}", act.getType(), agent.getId());
                ActivityType type = CarpoolingUtil.getActivityType(act);
                Id<Person> riderId = CarpoolingUtil.getRiderId(act);
                Id<Link> linkId = agent.getCurrentLinkId();
                MobsimPassengerAgent rider = (MobsimPassengerAgent) internalInterface.getMobsim().getAgents()
                        .get(riderId);
                double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
                switch (Objects.requireNonNull(type)) {
                    case dropoff:
                        int dropOffIndex = ((PlanAgent) agent).getCurrentPlan().getPlanElements().indexOf(act);
                        handleDropoff((MobsimDriverAgent) agent, rider, linkId, now, dropOffIndex);
                        break;
                    case pickup:
                        handlePickup((MobsimDriverAgent) agent, rider, linkId, now);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown activity " + type);
                }
            }
        }
        return false;
    }

    private void handleDropoff(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId,
            double now, int dropoffIndex) {
        if (!driver.getVehicle().getPassengers().contains(rider)) {
            LOGGER.debug("driver {} wanted to drop off rider {} on link {}, but it never entered the vehicle",
                    driver.getId(), rider.getId(), linkId);
            return;
        }
        LOGGER.debug("driver {} drops off rider {} on link {}", driver.getId(), rider.getId(), linkId);

        List<PlanElement> driverPlanElements = PopulationUtils
                .findPerson(driver.getId(), internalInterface.getMobsim().getScenario()).getSelectedPlan()
                .getPlanElements();
        Leg legWithRider = (Leg) driverPlanElements.get(dropoffIndex - 1);
        Leg legAfterRider = (Leg) driverPlanElements.get(dropoffIndex + 1);
        Leg legBeforeRider = (Leg) driverPlanElements.get(dropoffIndex - 3);
        CarpoolingUtil.setCarpoolingStatus(legWithRider, Carpooling.VALUE_STATUS_CARPOOLING);
        CarpoolingUtil.setCarpoolingStatus(legAfterRider, Carpooling.VALUE_STATUS_BEFORE_AFTER);
        CarpoolingUtil.setCarpoolingStatus(legBeforeRider, Carpooling.VALUE_STATUS_BEFORE_AFTER);

        Leg planElement = (Leg) ((PlanAgent) rider).getCurrentPlanElement();
        int legIndex = ((PlanAgent) rider).getCurrentPlan().getPlanElements().indexOf(planElement);
        List<PlanElement> riderPlanElements = PopulationUtils
                .findPerson(rider.getId(), internalInterface.getMobsim().getScenario()).getSelectedPlan()
                .getPlanElements();
        Leg leg = (Leg) riderPlanElements.get(legIndex);
        CarpoolingUtil.setCarpoolingStatus(leg, Carpooling.VALUE_STATUS_CARPOOLING);
        double distance = legWithRider.getRoute().getDistance();

        if (cfgGroup.getDriverProfitPerKm() != 0) {
            eventsManager.processEvent(new PersonMoneyEvent(
                    now,
                    driver.getId(),
                    cfgGroup.getDriverProfitPerKm() * distance / 1000d,
                    Carpooling.DRIVER_MODE + " profit",
                    rider.getId().toString(),
                    null));
        }

        driver.getVehicle().removePassenger(rider);
        rider.setVehicle(null);
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(now, rider.getId(), driver.getVehicle().getId()));
        rider.notifyArrivalOnLinkByNonNetworkMode(rider.getDestinationLinkId());
        rider.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(rider);
    }

    private void handlePickup(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId,
            double now) {
        if (!waitingRiders.getOrDefault(rider.getId(), Id.createLinkId(-1)).equals(linkId)) {
            LOGGER.warn("driver {} wanted to pick up rider {} at {} from link {}, but it was not there",
                    driver.getId(), rider.getId(), now, linkId);
            rider.setStateToAbort(now);
            return;
        }

        LOGGER.debug("driver {} picks up rider {} at {} from link {}", driver.getId(), rider.getId(), now, linkId);
        driver.getVehicle().addPassenger(rider);
        rider.setVehicle(driver.getVehicle());
        internalInterface.unregisterAdditionalAgentOnLink(rider.getId(), linkId);
        eventsManager
                .processEvent(new PersonEntersVehicleEvent(now, rider.getId(), driver.getVehicle().getId()));
        waitingRiders.remove(rider.getId());
    }

    @Override
    public void rescheduleActivityEnd(MobsimAgent agent) {
    }

}
