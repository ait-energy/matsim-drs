package at.ac.ait.matsim.domino.carpooling.passenger;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CarpoolingPassengerDepartureHandler implements DepartureHandler {
    private final String mode;
    private final InternalInterface internalInterface;
    private final DefaultAgentFactory agentFactory;
    private final HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests;
    private final EventsManager eventsManager;
    static List<Id<Person>> matchedPersons = new ArrayList<>();

    public CarpoolingPassengerDepartureHandler(String mode, InternalInterface internalInterface, DefaultAgentFactory agentFactory, HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests, EventsManager eventsManager) {
        this.mode = mode;
        this.internalInterface = internalInterface;
        this.agentFactory = agentFactory;
        this.matchedRequests = matchedRequests;
        this.eventsManager = eventsManager;
        for (CarpoolingRequest passengerRequest : matchedRequests.keySet()){
                matchedPersons.add(passengerRequest.getPerson().getId());
        }
    }

    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> id) {
        //check if the passenger mode is carpooling passenger
        if (!agent.getMode().equals(mode)){
            return false;
        }
        //check if this passenger is one of the matched passengers
        if (!matchedPersons.contains(agent.getId())){
            agent.setStateToAbort(now);
            return true;
        }

         //Todo: get the driver that should pickup this passenger
        MobsimDriverAgent driver = this.agentFactory.createMobsimAgentFromPerson(matchedRequests.get(getRightPassengerRequest( matchedRequests, agent)).getPerson());

        //Todo: check if the driver current location is similar to passengerRequest fromLink
        //Todo: put the passenger in the driver's vehicle
        if (driver.getCurrentLinkId().toString().equals(Objects.requireNonNull(getRightPassengerRequest(matchedRequests, agent)).getFromLink().getId().toString())){
            MobsimVehicle vehicle = driver.getVehicle();
            vehicle.addPassenger((MobsimPassengerAgent)agent);
            ((MobsimPassengerAgent) agent).setVehicle(vehicle);
            eventsManager.processEvent(new PersonEntersVehicleEvent(now, agent.getId(), vehicle.getId()));
            return true;
        }

        if (agent.getCurrentLinkId().toString().equals(Objects.requireNonNull(getRightPassengerRequest(matchedRequests, agent)).getToLink().getId().toString())){
            agent.notifyArrivalOnLinkByNonNetworkMode(agent.getDestinationLinkId());
            agent.endLegAndComputeNextState(now);
            internalInterface.arrangeNextAgentState(agent);
            return true;
        }
        return true;
    }

    //Todo: Loop over all the passenger requests, check which one of them has the same agentId and same destination
    private static CarpoolingRequest getRightPassengerRequest(HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests,  MobsimAgent agent){
        for (CarpoolingRequest passengerRequest : matchedRequests.keySet()){
            if (passengerRequest.getPerson().getId().equals(agent.getId())&&passengerRequest.getToLink().toString().equals(agent.getDestinationLinkId().toString())){
                return passengerRequest;
            }
        }
        return null;
    }
}
