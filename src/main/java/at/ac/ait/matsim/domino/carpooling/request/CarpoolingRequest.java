package at.ac.ait.matsim.domino.carpooling.request;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

public class CarpoolingRequest implements Request {
    private final Id<Request> id;
    private final Person person;
    private final Leg leg;
    private final double submissionTime;
    private final String mode;
    private final Coord origin;
    private final Coord destination;

    public CarpoolingRequest(Id<Request> id, Person person, Leg leg, double submissionTime, String mode, Coord origin, Coord destination) {
        this.id = id;
        this.person = person;
        this.leg = leg;
        this.submissionTime = submissionTime;
        this.mode = mode;
        this.origin = origin;
        this.destination = destination;
    }
    @Override
    public double getSubmissionTime() {
        return submissionTime;
    }
    @Override
    public Id<Request> getId() {
        return id;
    }
    public Coord getOrigin() {return origin;}
    public Coord getDestination() {return destination;}    public String getMode() {
        return mode;
    }
    public Person getPerson() {
        return person;
    }
    public Leg getLeg() {
        return leg;
    }
}
