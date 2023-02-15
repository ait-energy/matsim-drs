package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.Carpooling;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripStructureUtils;
import java.util.*;

public class RequestsCollector {
    private final List<CarpoolingRequest> driversRequests;
    private final List<CarpoolingRequest> passengersRequests;
    private final Population population;
    private final Network network;
    private long passengerRequestID = 0;
    private long driverRequestID = 0;

    public RequestsCollector(Population population, Network network) {
        this.population = population;
        this.network = network;
        driversRequests = new ArrayList<>();
        passengersRequests = new ArrayList<>();
    }

    public void collectRequests(){
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip: trips){
                List<Leg> legs = trip.getLegsOnly();
                for (Leg leg: legs) {
                    String mode = leg.getMode();
                    if (mode.equals(Carpooling.PASSENGER_MODE)) {
                        Activity startActivity = trip.getOriginActivity();
                        Link fromLink;
                        if (startActivity.getLinkId()==null){
                            fromLink = NetworkUtils.getNearestLink(network,startActivity.getCoord());
                        }else {
                            fromLink = network.getLinks().get(startActivity.getLinkId());
                        }
                        Activity endActivity = trip.getDestinationActivity();
                        Link toLink;
                        if (startActivity.getLinkId()==null){
                            toLink = NetworkUtils.getNearestLink(network, endActivity.getCoord());
                        }else {
                            toLink = network.getLinks().get(endActivity.getLinkId());
                        }
                        double activityEndTime = startActivity.getEndTime().seconds();
                        passengerRequestID = passengerRequestID+1;
                        CarpoolingRequest passengerRequest = new CarpoolingRequest(Id.create(passengerRequestID, Request.class), person, trip, activityEndTime,mode, fromLink, toLink);
                        passengersRequests.add(passengerRequest);
                    }
                    if (mode.equals(Carpooling.DRIVER_MODE)) {
                        Activity startActivity = trip.getOriginActivity();
                        Link fromLink;
                        if (startActivity.getLinkId()==null){
                            fromLink = NetworkUtils.getNearestLink(network,startActivity.getCoord());
                        }else {
                            fromLink = network.getLinks().get(startActivity.getLinkId());
                        }
                        Activity endActivity = trip.getDestinationActivity();
                        Link toLink;
                        if (startActivity.getLinkId()==null){
                            toLink = NetworkUtils.getNearestLink(network, endActivity.getCoord());
                        }else {
                            toLink = network.getLinks().get(endActivity.getLinkId());
                        }
                        double activityEndTime = startActivity.getEndTime().seconds();
                        driverRequestID = driverRequestID+1;
                        CarpoolingRequest driverRequest = new CarpoolingRequest(Id.create(driverRequestID, Request.class), person, trip, activityEndTime,mode, fromLink, toLink);
                        driversRequests.add(driverRequest);
                    }
                }
            }
        }
    }

    public List<CarpoolingRequest> getDriversRequests() {
        return driversRequests;
    }

    public List<CarpoolingRequest> getPassengersRequests() {
        return passengersRequests;
    }
}
