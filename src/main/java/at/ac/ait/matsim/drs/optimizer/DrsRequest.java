package at.ac.ait.matsim.drs.optimizer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Request for DRS trip. The given trip must be part of the person's selected
 * plan and only consist of a single leg. The from and to links must match the
 * given trip.
 */
public class DrsRequest implements Request {
    private final Id<Request> id;
    private final Person person;
    private final Trip trip;
    private final String mode;
    private final Link fromLink;
    private final Link toLink;
    private Leg legWithRoute;
    private Id<Request> matchedRequest;

    // only for unit tests, get rid of it in the future?
    private Double mockDepartureTime;
    private Leg mockLeg;

    /**
     * Request for being either driver or rider (see mode)
     */
    public DrsRequest(Id<Request> id, Person person, Trip trip, String mode, Link fromLink, Link toLink) {
        this.id = id;
        this.person = person;
        this.trip = trip;
        this.mode = mode;
        this.fromLink = fromLink;
        this.toLink = toLink;
        if (trip.getLegsOnly().size() > 1) {
            throw new IllegalArgumentException("trip contains more than one legs");
        }
    }

    /**
     * Request for being either driver or rider (see mode) - only use in unit tests!
     */
    public DrsRequest(Id<Request> id, Person person, Trip trip, double departureTime,
            String mode, Link fromLink, Link toLink, Leg leg) {
        this.mockLeg = leg;
        this.id = id;
        this.person = person;
        this.trip = trip;
        this.mockDepartureTime = departureTime;
        this.mode = mode;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

    public double getDepartureTime() {
        if (mockDepartureTime != null) {
            return mockDepartureTime;
        }
        return trip.getOriginActivity().getEndTime().seconds();
    }

    public String getMode() {
        return mode;
    }

    public Person getPerson() {
        return person;
    }

    public Trip getTrip() {
        return trip;
    }

    public Link getFromLink() {
        return fromLink;
    }

    public Link getToLink() {
        return toLink;
    }

    @Override
    public Id<Request> getId() {
        return id;
    }

    @Override
    public double getSubmissionTime() {
        return 0;
    }

    /**
     * actual leg of the underlying plan
     */
    public Leg getLeg() {
        if (mockLeg != null) {
            return mockLeg;
        }
        return trip.getLegsOnly().get(0);
    }

    /**
     * optional: pre-calculated leg with route (to avoid duplicate calculations).
     * can be null
     */
    public Leg getLegWithNetworkRoute() {
        return legWithRoute;
    }

    public void setLegWithNetworkRoute(Leg leg) {
        this.legWithRoute = leg;
    }

    /**
     * Get the distance on the street network or negative infinity if the request
     * does not have a street-network based leg
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
}
