package at.ac.ait.matsim.domino.carpooling;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import com.google.common.collect.ImmutableList;

public class Carpooling {
    public static final String DRIVER_MODE = "carpoolingDriver";
    public static final String PASSENGER_MODE = "carpoolingPassenger";

    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";
    public static final String PASSENGER_INTERACTION = PASSENGER_MODE + " interaction";

    public static final String PASSENGER_ID_ATTRIB = "passengerId";
    public static final String ACTIVITY_TYPE_ATTRIB = "type";

    public static enum ActivityType {
        pickup, dropoff
    };

    public static Id<Person> getPassengerId(Activity activity) {
        Object id = activity.getAttributes().getAttribute(PASSENGER_ID_ATTRIB);
        return id == null ? null : Id.createPersonId(id.toString());
    }

    public static void setPassengerId(Activity activity, Id<Person> id) {
        if (id != null) {
            activity.getAttributes().putAttribute(PASSENGER_ID_ATTRIB, id.toString());
        } else {
            activity.getAttributes().removeAttribute(PASSENGER_ID_ATTRIB);
        }
    }

    public static ActivityType getActivityType(Activity activity) {
        Object type = activity.getAttributes().getAttribute(ACTIVITY_TYPE_ATTRIB);
        return type == null ? null : ActivityType.valueOf(type.toString());
    }

    public static void setActivityType(Activity activity, ActivityType type) {
        if (type != null) {
            activity.getAttributes().putAttribute(ACTIVITY_TYPE_ATTRIB, type.toString());
        } else {
            activity.getAttributes().removeAttribute(ACTIVITY_TYPE_ATTRIB);
        }
    }
}
