package at.ac.ait.matsim.drs.optimizer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.OptionalTime;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Request for DRS trip. The given trip must be part of the person's selected
 * plan and only consist of a single leg. The from and to links must match the
 * given trip.
 */
public abstract class DrsRequest implements Request {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Id<Request> id;
    private final Person person;
    private final Trip trip;
    private final double departureTime;
    private final Link fromLink;
    private final Link toLink;
    private final Leg drsLeg;
    private Leg legWithRoute;
    private Id<Request> matchedRequest;

    /**
     * Creates a request from a trip. The trip must either consist of a single drs
     * leg or a drs leg accompanied by walk access and egress legs.
     *
     * @param drsNetwork must only consist of links accessible to the drsDriver mode
     * @return null in case no useful request can be created
     */
    public static DrsRequest create(Id<Request> id, RoutingModule driverRouter, Network drsNetwork, Person person,
            Trip trip) {
        String tripMode = TripStructureUtils.identifyMainMode(trip.getTripElements());
        if (!DrsUtil.isDrsMode(tripMode)) {
            return null;
        }

        List<Leg> nonWalkLegs = trip.getLegsOnly().stream().filter(l -> !l.getMode().equals(TransportMode.walk))
                .collect(Collectors.toList());
        if (nonWalkLegs.size() > 1) {
            LOGGER.warn("DRS trip for person {} contains more than one non-walk legs: {}", person.getId(),
                    nonWalkLegs.size());
            return null;
        }

        Leg drsLeg = nonWalkLegs.get(0);
        // in case of multiple legs the intermediate activities (access/egress)
        // they are start/end of our request
        int drsLegIdx = trip.getTripElements().indexOf(drsLeg);
        Activity startActivity = drsLegIdx == 0
                ? trip.getOriginActivity()
                : (Activity) trip.getTripElements().get(drsLegIdx - 1);
        Link fromLink = drsNetwork.getLinks().get(startActivity.getLinkId());
        Activity endActivity = drsLegIdx == trip.getTripElements().size() - 1
                ? trip.getDestinationActivity()
                : (Activity) trip.getTripElements().get(drsLegIdx + 1);
        Link toLink = drsNetwork.getLinks().get(endActivity.getLinkId());

        String msg = "Link {} ({}) not found in DRS network for person {}.";
        if (fromLink == null) {
            LOGGER.warn(msg, startActivity.getLinkId(), DrsUtil.toWktPoint(startActivity), person.getId());
            return null;
        } else if (toLink == null) {
            LOGGER.warn(msg, endActivity.getLinkId(), DrsUtil.toWktPoint(endActivity), person.getId());
            return null;
        }

        if (fromLink == toLink) {
            LOGGER.debug("Ignoring DRS request with equal from/to link ({}) for person {}", fromLink.getId(),
                    person.getId());
            return null;
        }
        double departureTime = findStartTime(trip, startActivity);

        DrsRequest request = tripMode.equals(Drs.DRIVER_MODE)
                ? new DrsDriverRequest(id, person, trip, departureTime, drsLeg, fromLink, toLink)
                : new DrsRiderRequest(id, person, trip, departureTime, drsLeg, fromLink, toLink);
        Leg driverLeg = DrsUtil.calculateLeg(driverRouter,
                request.getFromLink(),
                request.getToLink(),
                request.getDepartureTime(),
                request.getPerson());
        request.setLegWithNetworkRoute(driverLeg);
        return request;
    }

    private static Double findStartTime(Trip trip, Activity activity) {
        if (activity.getEndTime().isDefined()) {
            return activity.getEndTime().seconds();
        }
        // interaction activities don't have a start/end time,
        // so search for actual start activity in previous activities
        for (int i = trip.getTripElements().indexOf(activity) - 2; i >= 0; i -= 2) {
            Activity act = (Activity) trip.getTripElements().get(i);
            if (act.getEndTime().isDefined()) {
                return act.getEndTime().seconds();
            }
        }
        // last try: origin activity of trip itself
        return trip.getOriginActivity().getEndTime().seconds();
    }

    /**
     * Request for being either driver or rider (see mode)
     */
    public DrsRequest(Id<Request> id, Person person, Trip trip, double departureTime, Leg drsLeg, Link fromLink,
            Link toLink) {
        this.id = id;
        this.person = person;
        this.trip = trip;
        this.departureTime = departureTime;
        this.drsLeg = drsLeg;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

    public abstract String getMode();

    @Override
    public Id<Request> getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public Trip getTrip() {
        return trip;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    /**
     * @return leg of the person's plan that should contain the drs rider or driver
     *         route (and can be modified)
     */
    public Leg getDrsLeg() {
        return drsLeg;
    }

    public Link getFromLink() {
        return fromLink;
    }

    public Link getToLink() {
        return toLink;
    }

    public Node getFromNode() {
        return fromLink.getFromNode();
    }

    public Node getToNode() {
        return toLink.getToNode();
    }

    @Override
    public double getSubmissionTime() {
        return 0;
    }

    public Leg getLegWithNetworkRoute() {
        return legWithRoute;
    }

    /**
     * Set the route for this request (as calculated on the road network).
     * This does not change the person's trip but is
     * used to avoid multiple route calculations for this request
     *
     * @param leg
     */
    public void setLegWithNetworkRoute(Leg leg) {
        this.legWithRoute = leg;
    }

    /**
     * @return distance on the street network or negative infinity if the request
     *         does not yet have a street-network based leg
     */
    public double getNetworkRouteDistance() {
        if (legWithRoute != null) {
            if (legWithRoute.getRoute() instanceof NetworkRoute) {
                return legWithRoute.getRoute().getDistance();
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    public OptionalTime getNetworkRouteTravelTime() {
        if (legWithRoute != null) {
            if (legWithRoute.getRoute() instanceof NetworkRoute) {
                return legWithRoute.getRoute().getTravelTime();
            }
        }
        return OptionalTime.undefined();
    }

    public boolean isMatched() {
        return matchedRequest != null;
    }

    public Id<Request> getMatchedRequest() {
        return matchedRequest;
    }

    public void setMatchedRequest(Id<Request> matchedRequest) {
        this.matchedRequest = matchedRequest;
    }

    public static class DrsDriverRequest extends DrsRequest {
        public DrsDriverRequest(Id<Request> id, Person person, Trip trip, double departureTime, Leg drsLeg,
                Link fromLink, Link toLink) {
            super(id, person, trip, departureTime, drsLeg, fromLink, toLink);
        }

        @Override
        public String getMode() {
            return Drs.DRIVER_MODE;
        }

    }

    public static class DrsRiderRequest extends DrsRequest {
        public DrsRiderRequest(Id<Request> id, Person person, Trip trip, double departureTime, Leg drsLeg,
                Link fromLink, Link toLink) {
            super(id, person, trip, departureTime, drsLeg, fromLink, toLink);
        }

        @Override
        public String getMode() {
            return Drs.RIDER_MODE;
        }

    }
}
