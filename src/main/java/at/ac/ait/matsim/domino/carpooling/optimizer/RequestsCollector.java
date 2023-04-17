package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RequestsCollector {
    private static final Logger LOGGER = LogManager.getLogger();

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
                    if (!mode.equals(Carpooling.DRIVER_MODE) && !mode.equals(Carpooling.RIDER_MODE)) {
                        continue;
                    }

                    Activity startActivity = trip.getOriginActivity();
                    Link fromLink = network.getLinks().get(startActivity.getLinkId());
                    Activity endActivity = trip.getDestinationActivity();
                    Link toLink = network.getLinks().get(endActivity.getLinkId());
                    double activityEndTime = startActivity.getEndTime().seconds();

                    String msg = "leg {} ({}) not found in carpooling network for person {}.";
                    if (fromLink == null) {
                        LOGGER.warn(msg, startActivity.getLinkId(), CarpoolingUtil.toWktPoint(startActivity),
                                person.getId());
                        continue;
                    } else if (toLink == null) {
                        LOGGER.warn(msg, endActivity.getLinkId(), CarpoolingUtil.toWktPoint(endActivity),
                                person.getId());
                        continue;
                    }

                    if (mode.equals(Carpooling.RIDER_MODE)) {
                        riderRequestID = riderRequestID + 1;
                        CarpoolingRequest riderRequest = new CarpoolingRequest(Id.create(riderRequestID, Request.class),
                                person, trip, activityEndTime, mode, fromLink, toLink, leg);
                        ridersRequests.add(riderRequest);
                    } else if (mode.equals(Carpooling.DRIVER_MODE)) {
                        driverRequestID = driverRequestID + 1;
                        CarpoolingRequest driverRequest = new CarpoolingRequest(
                                Id.create(driverRequestID, Request.class), person, trip, activityEndTime, mode,
                                fromLink, toLink, leg);
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
