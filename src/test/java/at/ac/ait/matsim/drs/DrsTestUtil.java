package at.ac.ait.matsim.drs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.TripStructureUtils.Trip;

import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;

public class DrsTestUtil {
    public static DrsRiderRequest mockRiderRequest(int id, double departureTime) {
        return new DrsRiderRequest(Id.create(id, Request.class), null, null, departureTime, null, null, null);
    }

    public static DrsRiderRequest mockRiderRequest(int id, Person person, double departureTime, Trip trip, Leg leg) {
        return new DrsRiderRequest(Id.create(id, Request.class), person, trip, departureTime, leg, null, null);
    }

    public static DrsRiderRequest mockRiderRequest(int id, double departureTime, Link fromLink, Link toLink) {
        return new DrsRiderRequest(Id.create(id, Request.class), null, null, departureTime, null, fromLink, toLink);
    }

    public static DrsDriverRequest mockDriverRequest(int id, double departureTime, Link fromLink, Link toLink) {
        return new DrsDriverRequest(Id.create(id, Request.class), null, null, departureTime, null, fromLink, toLink);
    }
}
