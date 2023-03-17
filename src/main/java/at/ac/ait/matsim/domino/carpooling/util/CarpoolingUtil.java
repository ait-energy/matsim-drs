package at.ac.ait.matsim.domino.carpooling.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

public class CarpoolingUtil {

    public static void addNewAllowedModeToCarLinks(Network network, String newMode) {
        network.getLinks().values().forEach(l -> {
            if (l.getAllowedModes().contains(TransportMode.car)) {
                Set<String> modes = Sets.newHashSet(l.getAllowedModes());
                modes.add(newMode);
                l.setAllowedModes(modes);
            }
        });
    }

    public static void addMissingCoordsToPlanElementsFromLinks(Population population, Network network) {
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                List<Activity> activities = CarpoolingUtil.getActivities(plan.getPlanElements());
                CarpoolingUtil.addMissingCoordToActivitiesFromLink(activities, network);
            }
        }
    }

    static void addMissingCoordToActivitiesFromLink(Collection<Activity> activities, Network network) {
        for (Activity activity : activities) {
            if (activity.getCoord() == null && activity.getLinkId() != null) {
                Link link = network.getLinks().get(activity.getLinkId());
                activity.setCoord(link.getCoord());
            }
        }
    }

    public static List<Activity> getActivities(List<? extends PlanElement> elements) {
        return elements.stream() //
                .filter(Activity.class::isInstance) //
                .map(Activity.class::cast) //
                .collect(Collectors.toList());
    }

    public static Leg getFirstLeg(List<? extends PlanElement> planElements) {
        List<Leg> legs = TripStructureUtils.getLegs(planElements);
        if (legs.size() != 1) {
            throw new IllegalStateException("expected exactly one leg but got " + legs.size());
        }
        return legs.get(0);
    }

    /**
     * @return affinity string if present (emtpy string otherwise)
     */
    public static String getCarpoolingAffinity(Person person) {
        Object affinity = person.getAttributes().getAttribute(Carpooling.ATTRIB_AFFINITY);
        return affinity == null ? "" : affinity.toString();
    }

    public static Id<Person> getRiderId(Activity activity) {
        Object id = activity.getAttributes().getAttribute(Carpooling.ATTRIB_RIDER_ID);
        return id == null ? null : Id.createPersonId(id.toString());
    }

    public static void setRiderId(Activity activity, Id<Person> id) {
        if (id != null) {
            activity.getAttributes().putAttribute(Carpooling.ATTRIB_RIDER_ID, id.toString());
        } else {
            activity.getAttributes().removeAttribute(Carpooling.ATTRIB_RIDER_ID);
        }
    }

    public static Carpooling.ActivityType getActivityType(Activity activity) {
        Object type = activity.getAttributes().getAttribute(Carpooling.ATTRIB_ACTIVITY_TYPE);
        return type == null ? null : Carpooling.ActivityType.valueOf(type.toString());
    }

    public static void setActivityType(Activity activity, Carpooling.ActivityType type) {
        if (type != null) {
            activity.getAttributes().putAttribute(Carpooling.ATTRIB_ACTIVITY_TYPE, type.toString());
        } else {
            activity.getAttributes().removeAttribute(Carpooling.ATTRIB_ACTIVITY_TYPE);
        }
    }

    public static Double getActivityOriginalDepartureTime(Activity activity) {
        return (Double) activity.getAttributes().getAttribute(Carpooling.ATTRIB_ORIGINAL_DEP_TIME);
    }

    public static void setActivityOriginalDepartureTime(Activity activity, double originalDepartureTime) {
        activity.getAttributes().putAttribute(Carpooling.ATTRIB_ORIGINAL_DEP_TIME, originalDepartureTime);
    }

    // TODO refactor like get/setActivityType
    public static void removeActivityOriginalDepartureTime(Activity activity) {
        activity.getAttributes().removeAttribute(Carpooling.ATTRIB_ORIGINAL_DEP_TIME);
    }

    public static void setLinkageActivityToRiderRequest(CarpoolingRequest riderRequest) {
        for (PlanElement planElement : riderRequest.getPerson().getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                if (((Activity) planElement).getEndTime().isDefined()) {
                    if (((Activity) planElement).getEndTime().seconds() == riderRequest.getDepartureTime()) {
                        planElement.getAttributes().putAttribute(Carpooling.ATTRIB_LINKED_REQUEST,
                                riderRequest.getId().toString());
                        break;
                    }
                }
            }
        }
    }

    public static String getLinkageActivityToRiderRequest(Activity activity) {
        return (String) activity.getAttributes().getAttribute(Carpooling.ATTRIB_LINKED_REQUEST);
    }

    // TODO refactor like get/setActivityType
    public static void removeLinkageActivityToRiderRequest(Activity activity) {
        activity.getAttributes().removeAttribute(Carpooling.ATTRIB_LINKED_REQUEST);
    }

    public static void setRoutingMode(List<? extends PlanElement> legList) {
        for (PlanElement planElement : legList) {
            if (planElement instanceof Leg) {
                TripStructureUtils.setRoutingMode(((Leg) planElement), Carpooling.DRIVER_MODE);
            }
        }
    }
}
