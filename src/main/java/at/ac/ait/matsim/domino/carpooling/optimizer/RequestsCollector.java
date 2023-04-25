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

import com.google.common.collect.ImmutableList;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RequestsCollector {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Population population;
    private final Network network;
    private List<CarpoolingRequest> driverRequests;
    private List<CarpoolingRequest> riderRequests;

    public RequestsCollector(Population population, Network network) {
        this.population = population;
        this.network = network;
    }

    public void collectRequests() {
        driverRequests = new ArrayList<>();
        riderRequests = new ArrayList<>();

        long riderRequestID = 0;
        long driverRequestID = 0;

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
                        riderRequests.add(riderRequest);
                    } else if (mode.equals(Carpooling.DRIVER_MODE)) {
                        driverRequestID = driverRequestID + 1;
                        CarpoolingRequest driverRequest = new CarpoolingRequest(
                                Id.create(driverRequestID, Request.class), person, trip, activityEndTime, mode,
                                fromLink, toLink, leg);
                        driverRequests.add(driverRequest);
                    }
                }
            }
        }
        LOGGER.info("Collected {} driver and {} rider requests", driverRequests.size(), riderRequests.size());
    }

    public List<CarpoolingRequest> getDriverRequests() {
        return ImmutableList.copyOf(driverRequests);
    }

    public List<CarpoolingRequest> getRiderRequests() {
        return ImmutableList.copyOf(riderRequests);
    }
}
