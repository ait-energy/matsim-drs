package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.TripStructureUtils;
import java.util.*;

public class RequestsCollector {
    private final List<CarpoolingRequest> driversRequests;
    private final List<CarpoolingRequest> ridersRequests;
    private final Population population;
    private final Network network;
    private long riderRequestID = 0;
    private long driverRequestID = 0;

    public RequestsCollector(Population population, Network network) {
        this.population = population;
        this.network = network;
        driversRequests = new ArrayList<>();
        ridersRequests = new ArrayList<>();
    }

    public void collectRequests() {
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip : trips) {
                List<Leg> legs = trip.getLegsOnly();
                for (Leg leg : legs) {
                    String mode = leg.getMode();
                    if (mode.equals(Carpooling.RIDER_MODE)) {
                        Activity startActivity = trip.getOriginActivity();
                        Link fromLink = network.getLinks().get(startActivity.getLinkId());
                        Activity endActivity = trip.getDestinationActivity();
                        Link toLink = network.getLinks().get(endActivity.getLinkId());
                        double activityEndTime = startActivity.getEndTime().seconds();
                        riderRequestID = riderRequestID + 1;
                        CarpoolingRequest riderRequest = new CarpoolingRequest(Id.create(riderRequestID, Request.class),
                                person, trip, activityEndTime, mode, fromLink, toLink);
                        ridersRequests.add(riderRequest);
                    }
                    if (mode.equals(Carpooling.DRIVER_MODE)) {
                        Activity startActivity = trip.getOriginActivity();
                        Link fromLink = network.getLinks().get(startActivity.getLinkId());
                        Activity endActivity = trip.getDestinationActivity();
                        Link toLink = network.getLinks().get(endActivity.getLinkId());
                        double activityEndTime = startActivity.getEndTime().seconds();
                        driverRequestID = driverRequestID + 1;
                        CarpoolingRequest driverRequest = new CarpoolingRequest(
                                Id.create(driverRequestID, Request.class), person, trip, activityEndTime, mode,
                                fromLink, toLink);
                        driversRequests.add(driverRequest);
                    }
                }
            }
        }
    }

    public List<CarpoolingRequest> getDriversRequests() {
        return driversRequests;
    }

    public List<CarpoolingRequest> getRidersRequests() {
        return ridersRequests;
    }
}
