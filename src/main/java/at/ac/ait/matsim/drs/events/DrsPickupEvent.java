package at.ac.ait.matsim.drs.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class DrsPickupEvent extends Event {

    public final static String EVENT_TYPE = "drsPickup";

    private Id<Link> linkId;
    private Id<Person> driverId;
    private Id<Person> riderId;
    private Id<Vehicle> vehicleId;

    public DrsPickupEvent(double time, Id<Link> linkId, Id<Person> driverId, Id<Person> riderId,
            Id<Vehicle> vehicleId) {
        super(time);
        this.linkId = linkId;
        this.driverId = driverId;
        this.riderId = riderId;
        this.vehicleId = vehicleId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }

    public Id<Person> getDriverId() {
        return driverId;
    }

    public Id<Person> getRiderId() {
        return riderId;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    @Override
    public Map<String, String> getAttributes() {
        final Map<String, String> attributes = super.getAttributes();
        attributes.put("linkId", getLinkId().toString());
        attributes.put("driverId", getDriverId().toString());
        attributes.put("riderId", getRiderId().toString());
        attributes.put("vehicleId", getVehicleId().toString());
        return attributes;
    }
}
