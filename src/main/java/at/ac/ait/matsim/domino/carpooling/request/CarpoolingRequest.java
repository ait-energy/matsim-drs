package at.ac.ait.matsim.domino.carpooling.request;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.TripStructureUtils;

public class CarpoolingRequest implements Request {
    private boolean matched;
    private final Id<Request> id;
    private final Person person;
    private final TripStructureUtils.Trip trip;
    private final double departureTime;
    private final String mode;
    private final Link fromLink;
    private final Link toLink;

    public CarpoolingRequest(Id<Request> id, Person person, TripStructureUtils.Trip trip, double departureTime, String mode, Link fromLink, Link toLink) {
        this.matched = false;
        this.id = id;
        this.person = person;
        this.trip = trip;
        this.departureTime = departureTime;
        this.mode = mode;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

    public double getDepartureTime() {return departureTime;}
    public String getMode() {
        return mode;
    }
    public Person getPerson() {
        return person;
    }
    public TripStructureUtils.Trip getTrip() {
        return trip;
    }
    public Link getFromLink() {return fromLink;}
    public Link getToLink() {return toLink;}
    @Override
    public Id<Request> getId() {
        return id;
    }
    @Override
    public double getSubmissionTime() {
        return 0;
    }

    public boolean isMatched() {
        return matched;
    }
    public void setMatched(){
        this.matched=true;
    }

}
