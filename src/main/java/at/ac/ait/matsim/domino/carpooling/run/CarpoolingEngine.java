package at.ac.ait.matsim.domino.carpooling.run;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import at.ac.ait.matsim.domino.carpooling.passenger.CarpoolingPassengerDepartureHandler;

public class CarpoolingEngine implements MobsimEngine, ActivityHandler, DepartureHandler {

    public static final String COMPONENT_NAME = "carpoolingEngine";

    private Logger LOGGER = LogManager.getLogger();

    private InternalInterface internalInterface;
    private final EventsManager eventsManager;
    static List<Id<Person>> matchedPersons = new ArrayList<>();

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
        LOGGER.info("handleDeparture");
        return false;
        // // check if the passenger mode is carpooling passenger
        // if (!agent.getMode().equals(mode)) {
        // return false;
        // }
        // // check if this passenger is one of the matched passengers
        // if (!matchedPersons.contains(agent.getId())) {
        // agent.setStateToAbort(now);
        // return true;
        // }

        // // TODO: get the driver that should pickup this passenger
        // MobsimDriverAgent driver = null;
        // // this.agentFactory.createMobsimAgentFromPerson(
        // // matchedRequests.get(getRightPassengerRequest(matchedRequests,
        // // agent)).getPerson());

        // // Todo: check if the driver current location is similar to passengerRequest
        // // fromLink
        // // Todo: put the passenger in the driver's vehicle
        // if (driver.getCurrentLinkId().toString().equals(Objects
        // .requireNonNull(getRightPassengerRequest(matchedRequests,
        // agent)).getFromLink().getId().toString())) {
        // MobsimVehicle vehicle = driver.getVehicle();
        // vehicle.addPassenger((MobsimPassengerAgent) agent);
        // ((MobsimPassengerAgent) agent).setVehicle(vehicle);
        // eventsManager.processEvent(new PersonEntersVehicleEvent(now, agent.getId(),
        // vehicle.getId()));
        // return true;
        // }

        // if (agent.getCurrentLinkId().toString().equals(Objects
        // .requireNonNull(getRightPassengerRequest(matchedRequests,
        // agent)).getToLink().getId().toString())) {
        // agent.notifyArrivalOnLinkByNonNetworkMode(agent.getDestinationLinkId());
        // agent.endLegAndComputeNextState(now);

        // internalInterface.arrangeNextAgentState(agent);
        // return true;
        // }
        // return true;
    }

    @Override
    public boolean handleActivity(MobsimAgent agent) {
        LOGGER.info("handleActivity");
        return false;
    }

    @Override
    public void rescheduleActivityEnd(MobsimAgent agent) {
    }

}
