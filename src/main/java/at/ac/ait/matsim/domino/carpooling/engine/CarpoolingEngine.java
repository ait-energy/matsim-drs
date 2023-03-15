package at.ac.ait.matsim.domino.carpooling.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling.ActivityType;

/**
 * Heavily inspired by
 * org.matsim.contrib.dvrp.passenger.InternalPassengerHandling
 */
public class CarpoolingEngine implements MobsimEngine, ActivityHandler, DepartureHandler {

    public static final String COMPONENT_NAME = "carpoolingEngine";

    private final CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
    Logger LOGGER = LogManager.getLogger();
    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    private final Map<Id<Person>, Id<Link>> waitingRiders = new HashMap<>();

    @Inject
    public CarpoolingEngine(EventsManager eventsManager) {
        LOGGER.info("constructing new engine.");
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
        // TODO: get the matched and unmatched riders, matched riders get to wait,
        // unmatched are aborted or teleported
        LOGGER.debug("handleDeparture agent {} on link {}", agent.getId(), id);
        if (agent.getMode().equals(Carpooling.RIDER_MODE)) {
            waitingRiders.put(agent.getId(), id);
            return true;
        }
        return false;
    }

    /**
     * Will always return false so that the default activity handler properly
     * handles the activities of the driver.
     * 
     * Not sure if this is a clean approach, but duplicating code from
     * ActivityEngineDefaultImpl.handleActivity seems not so great too.
     */
    @Override
    public boolean handleActivity(MobsimAgent agent) {
        LOGGER.debug("handleActivity agent {}", agent.getId());
        if (agent instanceof PlanAgent && agent instanceof MobsimDriverAgent) {
            Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
            if (act.getType().equals(Carpooling.DRIVER_INTERACTION)) {
                ActivityType type = CarpoolingUtil.getActivityType(act);
                Id<Person> riderId = CarpoolingUtil.getRiderId(act);
                Id<Link> linkId = agent.getCurrentLinkId();
                MobsimPassengerAgent rider = (MobsimPassengerAgent) internalInterface.getMobsim().getAgents()
                        .get(riderId);
                double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
                switch (Objects.requireNonNull(type)) {
                    case dropoff:
                        // TODO: Get the previous leg and calculate its distance, add the distance to
                        // handleDropOff() method
                        Leg previousLeg = (Leg) ((PlanAgent) agent).getPreviousPlanElement();
                        double distance = previousLeg.getRoute().getDistance();
                        handleDropoff((MobsimDriverAgent) agent, rider, linkId, now, distance);
                        break;
                    case pickup:
                        handlePickup((MobsimDriverAgent) agent, rider, linkId, now);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown activity " + type);
                }
            } else if (act.getType().equals(Carpooling.RIDER_INTERACTION)) {
                // TODO not necessary to handle this?
                LOGGER.debug("handleActivity {} {}", act.getType(), agent.getId());
            }
        }
        return false;
    }

    private void handleDropoff(MobsimDriverAgent driver, MobsimPassengerAgent rider, Id<Link> linkId,
            double now, double distance) {
        if (!driver.getVehicle().getPassengers().contains(rider)) {
            LOGGER.debug("driver {} wanted to drop off rider {} on link {}, but it never entered the vehicle",
                    driver.getId(), rider.getId(), linkId);
            return;
        }
        LOGGER.debug("driver {} drops off rider {} on link {}", driver.getId(), rider.getId(), linkId);

        // TODO: eventsManager.processEvent(new MoneyEvent(for the driver and the rider?
        // using the distance travelled))
        eventsManager.processEvent(new PersonMoneyEvent(now, driver.getId(),
                (cfgGroup.driverMoneyPerKM / 1000) * distance, "Carpooling", rider.getId().toString(), null));
        eventsManager.processEvent(new PersonMoneyEvent(now, rider.getId(),
                (cfgGroup.riderMoneyPerKM / 1000) * distance, "Carpooling", driver.getId().toString(), null));

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
            LOGGER.debug("driver {} wanted to pick up rider {} at {} from link {}, but it was not there",
                    driver.getId(), rider.getId(), now, linkId);
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
