package at.ac.ait.matsim.drs.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.google.common.collect.Sets;

import at.ac.ait.matsim.drs.replanning.PermissibleModesCalculatorForDrs;
import at.ac.ait.matsim.drs.run.Drs;

public class DrsUtil {

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
                List<Activity> activities = DrsUtil.getActivities(plan.getPlanElements());
                DrsUtil.addMissingCoordToActivitiesFromLink(activities, network);
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

    public static String getDrsAffinity(Person person) {
        Object affinity = person.getAttributes().getAttribute(Drs.ATTRIB_AFFINITY);
        return affinity == null ? "" : affinity.toString();
    }

    public static Id<Person> getRiderId(Activity activity) {
        Object id = activity.getAttributes().getAttribute(Drs.ATTRIB_RIDER_ID);
        return id == null ? null : Id.createPersonId(id.toString());
    }

    public static void setRiderId(Activity activity, Id<Person> id) {
        if (id != null) {
            activity.getAttributes().putAttribute(Drs.ATTRIB_RIDER_ID, id.toString());
        } else {
            activity.getAttributes().removeAttribute(Drs.ATTRIB_RIDER_ID);
        }
    }

    public static Drs.ActivityType getActivityType(Activity activity) {
        Object type = activity.getAttributes().getAttribute(Drs.ATTRIB_ACTIVITY_TYPE);
        return type == null ? null : Drs.ActivityType.valueOf(type.toString());
    }

    public static void setActivityType(Activity activity, Drs.ActivityType type) {
        if (type != null) {
            activity.getAttributes().putAttribute(Drs.ATTRIB_ACTIVITY_TYPE, type.toString());
        } else {
            activity.getAttributes().removeAttribute(Drs.ATTRIB_ACTIVITY_TYPE);
        }
    }

    public static Double getActivityOriginalDepartureTime(Activity activity) {
        return (Double) activity.getAttributes().getAttribute(Drs.ATTRIB_ORIGINAL_DEP_TIME);
    }

    public static void setActivityOriginalDepartureTime(Activity activity, Double originalDepartureTime) {
        if (originalDepartureTime != null) {
            activity.getAttributes().putAttribute(Drs.ATTRIB_ORIGINAL_DEP_TIME, originalDepartureTime);
        } else {
            activity.getAttributes().removeAttribute(Drs.ATTRIB_ORIGINAL_DEP_TIME);
        }
    }

    public static String getLinkageActivityToRiderRequest(Activity activity) {
        return (String) activity.getAttributes().getAttribute(Drs.ATTRIB_LINKED_REQUEST);
    }

    public static void setLinkageActivityToRiderRequest(Activity activity, String riderRequestId) {
        if (riderRequestId != null) {
            activity.getAttributes().putAttribute(Drs.ATTRIB_LINKED_REQUEST, riderRequestId);
        } else {
            activity.getAttributes().removeAttribute(Drs.ATTRIB_LINKED_REQUEST);
        }
    }

    public static void setRoutingModeToDriver(List<? extends PlanElement> legList) {
        for (PlanElement planElement : legList) {
            if (planElement instanceof Leg) {
                TripStructureUtils.setRoutingMode(((Leg) planElement), Drs.DRIVER_MODE);
            }
        }
    }

    public static int addDriverPlanForEligibleAgents(Population population, Config config,
            String... excludedSubpopulations) {
        PermissibleModesCalculatorForDrs permissible = new PermissibleModesCalculatorForDrs(config);
        int count = 0;
        Set<String> excludedSubpopulationSet = Sets.newHashSet(excludedSubpopulations);
        for (Person person : population.getPersons().values()) {
            Object subpop = person.getAttributes().getAttribute("subpopulation");
            if (subpop != null && excludedSubpopulationSet.contains(subpop.toString())) {
                continue;
            }

            Plan plan = person.getSelectedPlan();
            if (permissible.getPermissibleModes(plan).contains(Drs.DRIVER_MODE)) {
                Plan newPlan = PopulationUtils.createPlan();
                PopulationUtils.copyFromTo(plan, newPlan);

                for (Trip trip : TripStructureUtils.getTrips(newPlan)) {
                    TripRouter.insertTrip(
                            newPlan,
                            trip.getOriginActivity(),
                            Collections.singletonList(PopulationUtils.createLeg(Drs.DRIVER_MODE)),
                            trip.getDestinationActivity());
                }

                newPlan.setPerson(person);
                person.addPlan(newPlan);
                count++;
            }
        }
        return count;
    }

    /**
     * @return a route as a single leg (as expected when routing with a network
     *         mode)
     * @throws IllegalStateException if the resulting route contains more legs
     */
    public static Leg calculateLeg(RoutingModule router, Link from, Link to, double departureTime, Person driver) {
        return calculateLeg(router,
                FacilitiesUtils.wrapLink(from),
                FacilitiesUtils.wrapLink(to),
                departureTime,
                driver);
    }

    /**
     * @return a route as a single leg (as expected when routing with a network
     *         mode)
     * @throws IllegalStateException if the resulting route contains more legs
     */
    public static Leg calculateLeg(RoutingModule router, Facility from, Facility to, double departureTime,
            Person driver) {
        RoutingRequest routingRequest = DefaultRoutingRequest.withoutAttributes(from, to, departureTime, driver);
        List<? extends PlanElement> legList = router.calcRoute(routingRequest);
        routeCalculations.incrementAndGet();
        return getFirstLeg(legList);
    }

    public static volatile AtomicInteger routeCalculations = new AtomicInteger();

    /**
     * @throws IllegalStateException if more legs are contained
     */
    public static Leg getFirstLeg(List<? extends PlanElement> planElements) {
        List<Leg> legs = TripStructureUtils.getLegs(planElements);
        if (legs.size() != 1) {
            throw new IllegalStateException("expected exactly one leg but got " + legs.size());
        }
        return legs.get(0);
    }

    public static String getRequestStatus(Leg leg) {
        return (String) leg.getAttributes().getAttribute(Drs.ATTRIB_REQUEST_STATUS);
    }

    public static void setRequestStatus(Leg leg, String status) {
        if (status != null) {
            leg.getAttributes().putAttribute(Drs.ATTRIB_REQUEST_STATUS, status);
        } else {
            leg.getAttributes().removeAttribute(Drs.ATTRIB_REQUEST_STATUS);
        }
    }

    public static String getDrsStatus(Leg leg) {
        return (String) leg.getAttributes().getAttribute(Drs.ATTRIB_DRS_STATUS);
    }

    public static void setDrsStatus(Leg leg, String status) {
        if (status != null) {
            leg.getAttributes().putAttribute(Drs.ATTRIB_DRS_STATUS, status);
        } else {
            leg.getAttributes().removeAttribute(Drs.ATTRIB_DRS_STATUS);
        }
    }

    public static String toWktPoint(Activity activity) {
        if (activity == null || activity.getCoord() == null) {
            return "POINT EMPTY";
        }
        return String.format("POINT(%.1f %.1f)", activity.getCoord().getX(), activity.getCoord().getY());
    }

    public static boolean writeGraph(AfterMobsimEvent event, ControllerConfigGroup controllerConfigGroup) {
        int createGraphsInterval = controllerConfigGroup.getCreateGraphsInterval();
        return createGraphsInterval > 0 && event.getIteration() % createGraphsInterval == 0;
    }

}
