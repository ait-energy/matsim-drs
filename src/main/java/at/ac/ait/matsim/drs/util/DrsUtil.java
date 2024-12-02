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
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.google.common.collect.Sets;

import at.ac.ait.matsim.drs.engine.PermissibleModesCalculatorForDrs;
import at.ac.ait.matsim.drs.run.Drs;

public class DrsUtil {

    public static boolean isDrsMode(String mode) {
        return mode.equals(Drs.DRIVER_MODE) || mode.equals(Drs.RIDER_MODE);
    }

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

    public static void setRoutingModeToDriver(List<? extends PlanElement> legList) {
        for (PlanElement planElement : legList) {
            if (planElement instanceof Leg) {
                TripStructureUtils.setRoutingMode(((Leg) planElement), Drs.DRIVER_MODE);
            }
        }
    }

    public static int addDrsDriverPlans(Population population, Config config,
            String... excludedSubpopulations) {
        return addDrsPlanForEligiblePlans(population, config, Drs.DRIVER_MODE, Set.of(TransportMode.car),
                excludedSubpopulations);
    }

    /**
     * Copy the currently selected plan, replace eligible trips with target drs
     * mode, and set the new plan as selected.
     */
    public static int addDrsPlanForEligiblePlans(Population population, Config config, String targetDrsMode,
            Set<String> modesToReplace, String... excludedSubpopulations) {
        PermissibleModesCalculatorForDrs permissible = new PermissibleModesCalculatorForDrs(config);
        int count = 0;
        Set<String> excludedSubpopulationSet = Sets.newHashSet(excludedSubpopulations);
        for (Person person : population.getPersons().values()) {
            Object subpop = person.getAttributes().getAttribute("subpopulation");
            if (subpop != null && excludedSubpopulationSet.contains(subpop.toString())) {
                continue;
            }

            Plan plan = person.getSelectedPlan();
            if (permissible.getPermissibleModes(plan).contains(targetDrsMode)) {
                Plan newPlan = PopulationUtils.createPlan();
                PopulationUtils.copyFromTo(plan, newPlan);

                // replace specific trips only
                boolean featuresNewDrsMode = false;
                for (Trip trip : TripStructureUtils.getTrips(newPlan)) {
                    String tripMode = TripStructureUtils.identifyMainMode(trip.getTripElements());
                    if (modesToReplace.contains(tripMode)) {
                        featuresNewDrsMode = true;
                        TripRouter.insertTrip(
                                newPlan,
                                trip.getOriginActivity(),
                                Collections.singletonList(PopulationUtils.createLeg(targetDrsMode)),
                                trip.getDestinationActivity());
                    }
                }

                if (featuresNewDrsMode) {
                    newPlan.setPerson(person);
                    person.addPlan(newPlan);
                    person.setSelectedPlan(newPlan);
                    count++;
                }
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

    public static String getAssignedDriver(Leg leg) {
        return (String) leg.getAttributes().getAttribute(Drs.ATTRIB_ASSIGNED_DRIVER);
    }

    public static void setAssignedDriver(Leg leg, String personId) {
        if (personId != null) {
            leg.getAttributes().putAttribute(Drs.ATTRIB_ASSIGNED_DRIVER, personId);
        } else {
            leg.getAttributes().removeAttribute(Drs.ATTRIB_ASSIGNED_DRIVER);
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

    public static boolean writeGraph(int iteration, ControllerConfigGroup controllerConfigGroup) {
        int createGraphsInterval = controllerConfigGroup.getCreateGraphsInterval();
        return createGraphsInterval > 0 && iteration % createGraphsInterval == 0;
    }

    /**
     * Warning: only use this for testing purposes. This can be useful for hardcoded
     * drsRider legs in an initial population. Since we usually don't want to have a
     * teleporting routing config we simply set a fake route so that
     * PersonPrepareForSim does not try to create a route for these legs (because it
     * will fail due to no avilable router).
     */
    public static void addFakeGenericRouteToDrsDriverLegs(Population population) {
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (Leg leg : TripStructureUtils.getLegs(plan.getPlanElements())) {
                    if (leg.getMode().equals(Drs.RIDER_MODE)) {
                        if (leg.getRoute() == null) {
                            int idx = plan.getPlanElements().indexOf(leg);
                            Activity fromActivity = (Activity) plan.getPlanElements().get(idx - 1);
                            Activity toActivity = (Activity) plan.getPlanElements().get(idx + 1);
                            TripStructureUtils.setRoutingMode(leg, Drs.RIDER_MODE);
                            Route fake = new GenericRouteImpl(fromActivity.getLinkId(), toActivity.getLinkId());
                            fake.setDistance(1);
                            fake.setTravelTime(1);
                            leg.setRoute(fake);
                        }
                    }
                }
            }
        }
    }

}
