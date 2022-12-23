package at.ac.ait.matsim.domino.carpooling.request;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

public class CarpoolingRequest implements Request {

    private final Id<Request> id;

    private final double submissionTime;

    private final Id<Person> driverId;

    private final String mode;

    private final Link fromLink;

    private final Link toLink;

    public CarpoolingRequest(Id<Request> id, double submissionTime, Id<Person> driverId, String mode, Link fromLink,
            Link toLink) {
        this.id = id;
        this.submissionTime = submissionTime;
        this.driverId = driverId;
        this.mode = mode;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

    @Override
    public double getSubmissionTime() {
        return submissionTime;
    }

    @Override
    public Id<Request> getId() {
        return id;
    }

    public Id<Person> getDriverId() {
        return driverId;
    }

    public String getMode() {
        return mode;
    }

    public Link getFromLink() {
        return fromLink;
    }

    public Link getToLink() {
        return toLink;
    }
}
