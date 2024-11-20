package at.ac.ait.matsim.drs.optimizer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

public class DrsRequest implements Request {
    private final Id<Request> id;
    private final Person person;
    private final TripStructureUtils.Trip trip;
    private final double departureTime;
    private final String mode;
    private final Link fromLink;
    private final Link toLink;
    private final Leg leg;
    private Leg legWithRoute;

    /**
     * Request for being either driver or rider (see mode)
     */
    public DrsRequest(Id<Request> id, Person person, TripStructureUtils.Trip trip, double departureTime,
            String mode, Link fromLink, Link toLink, Leg leg) {
        this.leg = leg;
        this.id = id;
        this.person = person;
        this.trip = trip;
        this.departureTime = departureTime;
        this.mode = mode;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    public String getMode() {
        return mode;
    }

    public Person getPerson() {
        return person;
    }

    public TripStructureUtils.Trip getTrip() {
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
     * original leg (from the plans file)
     */
    public Leg getLeg() {
        return leg;
    }

    /**
     * optional: pre-calculated leg with route (to avoid duplicate calculations).
     * can be null
     */
    public Leg getLegWithRoute() {
        return legWithRoute;
    }

    public void setLegWithRoute(Leg leg) {
        this.legWithRoute = leg;
    }

    /**
     * Get the travel time on the street network or negative infinity if the request
     * does not have a street-network based leg
     */
    public double getNetworkRouteDistance() {
        if (legWithRoute != null) {
            if (legWithRoute.getRoute() instanceof NetworkRoute) {
                return legWithRoute.getRoute().getDistance();
            }
        }
        if (leg.getRoute() instanceof NetworkRoute) {
            return leg.getRoute().getDistance();
        }
        return Double.NEGATIVE_INFINITY;
    }

}
