package at.ac.ait.matsim.domino.carpooling.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
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

import at.ac.ait.matsim.domino.carpooling.Carpooling;
import at.ac.ait.matsim.domino.carpooling.Carpooling.ActivityType;
import at.ac.ait.matsim.domino.carpooling.passenger.CarpoolingPassengerDepartureHandler;

/**
 * Heavily inspired by
 * org.matsim.contrib.dvrp.passenger.InternalPassengerHandling
 */
public class CarpoolingEngine implements MobsimEngine, ActivityHandler, DepartureHandler {

    public static final String COMPONENT_NAME = "carpoolingEngine";

    private Logger LOGGER = LogManager.getLogger();

    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    static List<Id<Person>> matchedPersons = new ArrayList<>();

    private Map<Id<Person>, Id<Link>> waitingPassengers = new HashMap<>();

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
        LOGGER.debug("handleDeparture agent {} on link {}", agent.getId(), id);
        if (agent.getMode().equals(Carpooling.PASSENGER_MODE)) {
            LOGGER.info("agent {} waits on link {} for carpooling driver", agent.getId(), id);
            waitingPassengers.put(agent.getId(), id);
            return true;
        }
        return false;
    }

    /**
     * Will always return false so that the default activity handler properly
     * handles the activities of the driver.
     * 
     * Not sure if this is a clean approach, but duplicating code from
     * ActivityEngineDefaultImpl.handleActivitiy seems .. not so great too.
     */
    @Override
    public boolean handleActivity(MobsimAgent agent) {
        LOGGER.debug("handleActivity agent {}", agent.getId());
        if (agent instanceof PlanAgent && agent instanceof MobsimDriverAgent) {
            Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
            if (act.getType().equals(Carpooling.DRIVER_INTERACTION)) {
                ActivityType type = Carpooling.getActivityType(act);
                Id<Person> passengerId = Carpooling.getPassengerId(act);
                Id<Link> linkId = agent.getCurrentLinkId();
                MobsimPassengerAgent passenger = (MobsimPassengerAgent) internalInterface.getMobsim().getAgents()
                        .get(passengerId);
                double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
                switch (type) {
                    case dropoff:
                        handleDropoff((MobsimDriverAgent) agent, passenger, linkId, now);
                        break;
                    case pickup:
                        handlePickup((MobsimDriverAgent) agent, passenger, linkId, now);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown activity " + type);
                }
            } else if (act.getType().equals(Carpooling.PASSENGER_INTERACTION)) {
                // TODO not necessary to handle this?
                LOGGER.info("handleActivity {} {}", act.getType(), agent.getId());
            }
        }
        return false;
    }

    private void handleDropoff(MobsimDriverAgent driver, MobsimPassengerAgent passenger, Id<Link> linkId,
            double now) {
        if (!driver.getVehicle().getPassengers().contains(passenger)) {
            LOGGER.info(
                    "driver {} wanted to drop off passenger {} on link {}, but it never entered the vehicle",
                    driver.getId(), passenger.getId(), linkId);
            return;
        }

        LOGGER.info("driver {} drops off passenger {} on link {}", driver.getId(), passenger.getId(), linkId);
        driver.getVehicle().removePassenger(passenger);
        passenger.setVehicle(null);
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(now, passenger.getId(), driver.getVehicle().getId()));
        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(passenger);
    }

    private void handlePickup(MobsimDriverAgent driver, MobsimPassengerAgent passenger, Id<Link> linkId,
            double now) {
        if (!waitingPassengers.getOrDefault(passenger.getId(), Id.createLinkId(-1)).equals(linkId)) {
            LOGGER.info("driver {} wanted to pick up passenger {} from link {}, but it was not there",
                    driver.getId(), passenger.getId(), linkId);
            return;
        }

        LOGGER.info("driver {} picks up passenger {} from link {}", driver.getId(), passenger.getId(),
                linkId);
        // TODO is this required?
        // if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(),
        // driver.getCurrentLinkId()) == null) {
        // // only possible with prebooking
        // return false;
        // }
        driver.getVehicle().addPassenger(passenger);
        passenger.setVehicle(driver.getVehicle());
        internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), linkId);
        eventsManager
                .processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), driver.getVehicle().getId()));
        waitingPassengers.remove(passenger.getId());
    }

    @Override
    public void rescheduleActivityEnd(MobsimAgent agent) {
    }

}
