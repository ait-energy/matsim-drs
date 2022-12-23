package at.ac.ait.matsim.salabim.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.ActivityFacility;

import com.google.common.collect.ImmutableSet;

// import at.ac.ait.matsim.salabim.routing.SalabimConfigGroup;

public class SalabimUtil {

    public static final ZoneId ZONE_ID = ZoneId.systemDefault();

    // public static SalabimConfigGroup addOrGetSalabimConfigGroup(Scenario
    // scenario) {
    // return ConfigUtils.addOrGetModule(scenario.getConfig(),
    // SalabimConfigGroup.GROUP_NAME,
    // SalabimConfigGroup.class);
    // }

    // public static SalabimConfigGroup addOrGetSalabimConfigGroup(Config config) {
    // return ConfigUtils.addOrGetModule(config, SalabimConfigGroup.GROUP_NAME,
    // SalabimConfigGroup.class);
    // }

    /**
     * @return the path where the scenario, i.e. the config.xml, population etc.,
     *         are located
     */
    public static Path getScenarioPath(Scenario scenario) {
        URL cacheURL = ConfigGroup.getInputFileURL(scenario.getConfig().getContext(), ".");
        try {
            return Paths.get(cacheURL.toURI()).normalize();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated use {@link OutputDirectoryHierarchy} instead
     */
    @Deprecated
    public static Path getOutputPath(Scenario scenario) {
        return Paths.get(scenario.getConfig().controler().getOutputDirectory()).toAbsolutePath().normalize();
    }

    public static double getTotalDistanceM(List<? extends PlanElement> elements) {
        double length = 0;
        for (PlanElement element : elements) {
            if (element instanceof Leg) {
                Leg leg = (Leg) element;
                if (leg.getRoute() != null) {
                    length += leg.getRoute().getDistance();
                }
            }
        }
        return length;
    }

    /**
     * Extract the link id of an activity in the following order - returning the
     * first match:
     * <ol>
     * <li>link id of activity</li>
     * <li>link id of facility</li>
     * <li>nearest link to coordinates of activity</li>
     * <li>nearest link to coordinates of facility</li>
     * </ol>
     * 
     * @return an id or <code>null</code>
     */
    public static Id<Link> getLinkId(Scenario scenario, Activity activity) {
        if (activity.getLinkId() != null)
            return activity.getLinkId();

        ActivityFacility facility = null;
        if (activity.getFacilityId() != null) {
            facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
        }

        if (facility != null && facility.getLinkId() != null) {
            return facility.getLinkId();
        }

        if (activity.getCoord() != null) {
            return NetworkUtils.getNearestLink(scenario.getNetwork(), activity.getCoord()).getId();
        }

        if (facility != null && facility.getCoord() != null) {
            return NetworkUtils.getNearestLink(scenario.getNetwork(), facility.getCoord()).getId();
        }

        return null;
    }

    public static List<Activity> getActivities(List<? extends PlanElement> elements) {
        return elements.stream() //
                .filter(Activity.class::isInstance) //
                .map(Activity.class::cast) //
                .collect(Collectors.toList());
    }

    /**
     * @return all real activities excluding so-called stage activities such as "pt
     *         interaction" (sometimes also called dummy activities)
     */
    public static List<Activity> getRealActivities(List<? extends PlanElement> elements) {
        return getActivities(elements).stream()//
                .filter(a -> !StageActivityTypeIdentifier.isStageActivity(a.getType()))//
                .collect(Collectors.toList());
    }

    /**
     * @return all stating activities such as "pt interaction" (sometimes also
     *         called dummy activities)
     */
    public static List<Activity> getStagingActivities(List<? extends PlanElement> elements) {
        return getActivities(elements).stream()//
                .filter(a -> StageActivityTypeIdentifier.isStageActivity(a.getType()))//
                .collect(Collectors.toList());
    }

    /**
     * Get the first {@link Activity} that matches one of the provided types
     */
    public static Optional<Activity> getFirstActivity(List<? extends PlanElement> elements, String... activityType) {
        Set<String> types = ImmutableSet.copyOf(activityType);
        return getActivities(elements).stream()//
                .filter(activity -> types.contains(activity.getType()))//
                .findFirst();
    }

    public static List<String> getActivityTypes(List<? extends PlanElement> elements) {
        return getActivities(elements).stream() //
                .map(Activity::getType) //
                .collect(Collectors.toList());
    }

    public static List<Leg> getLegs(Plan plan) {
        return getLegs(plan.getPlanElements());
    }

    public static List<Leg> getLegs(List<? extends PlanElement> elements) {
        return elements.stream() //
                .filter(Leg.class::isInstance) //
                .map(Leg.class::cast) //
                .collect(Collectors.toList());
    }

    /**
     * Get the first {@link Leg} that matches one of the provided modes. If no mode
     * is specified all modes are considered a match.
     */
    public static Optional<Leg> getFirstLeg(List<? extends PlanElement> elements, String... modes) {
        Set<String> theModes = ImmutableSet.copyOf(modes);
        Stream<Leg> stream = getLegs(elements).stream();
        if (!theModes.isEmpty()) {
            stream = stream.filter(leg -> theModes.contains(leg.getMode()));
        }
        return stream.findFirst();
    }

    /**
     * Get the last {@link Leg} that matches one of the provided modes. If no mode
     * is specified all modes are considered a match.
     */
    public static Optional<Leg> getLastLeg(List<? extends PlanElement> elements, String... modes) {
        Set<String> theModes = ImmutableSet.copyOf(modes);
        Stream<Leg> stream = getLegs(elements).stream();
        if (!theModes.isEmpty()) {
            stream = stream.filter(leg -> theModes.contains(leg.getMode()));
        }
        List<Leg> matchingLegs = stream.collect(Collectors.toList());
        if (matchingLegs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matchingLegs.get(matchingLegs.size() - 1));
    }

    /**
     * @return the modes of all legs in the same order as the legs given as input
     *         (and including duplicates)
     */
    public static List<String> getLegModes(List<? extends PlanElement> elements) {
        return getLegs(elements).stream() //
                .map(Leg::getMode) //
                .collect(Collectors.toList());
    }

    /**
     * Set a routingMode for each leg. This is required in MATSim 12 (since
     * https://github.com/matsim-org/matsim/pull/738)
     */
    public static void setRoutingMode(List<? extends PlanElement> elements, String routingMode) {
        for (Leg leg : SalabimUtil.getLegs(elements)) {
            TripStructureUtils.setRoutingMode(leg, routingMode);
        }
    }

    /**
     * Get all valid routing modes contained in the legs
     */
    public static Set<String> getRoutingModes(List<? extends PlanElement> elements) {
        Set<String> routingModes = new HashSet<>();
        for (Leg leg : SalabimUtil.getLegs(elements)) {
            String routingMode = TripStructureUtils.getRoutingMode(leg);
            if (routingMode != null) {
                routingModes.add(routingMode);
            }
        }
        return routingModes;
    }

    /**
     * known activity types
     */
    public enum ActivityType {
        EDUCATION, ERRAND, HOME, LEISURE, SHOPPING, WORK;

        /**
         * @return id as to be used in the population / plans.xml
         */
        public String getStringForPlans() {
            return name().toLowerCase();
        }

        public static ActivityType fromStringInPlans(String type) {
            return ActivityType.valueOf(type.toUpperCase());
        }

    }

    public static class LegWithActivities {
        public final Activity startActivity;
        public final Leg leg;
        public final Activity endActivity;

        public LegWithActivities(Activity startActivity, Leg leg, Activity endActivity) {
            this.startActivity = startActivity;
            this.leg = leg;
            this.endActivity = endActivity;
        }

    }

    public static LegWithActivities getActivitiesForLeg(List<? extends PlanElement> elements, Leg leg) {
        int i = elements.indexOf(leg);
        return new LegWithActivities((Activity) elements.get(i - 1), leg, (Activity) elements.get(i + 1));
    }

    public static List<Leg> getLegsForMode(List<? extends PlanElement> elements, String mode) {
        return elements.stream() //
                .filter(Leg.class::isInstance) //
                .map(Leg.class::cast) //
                .filter(l -> l.getMode().equals(mode)) //
                .collect(Collectors.toList());
    }

    public static void replaceModeForLegs(List<? extends PlanElement> elements, String oldMode, String newMode) {
        elements.stream() //
                .filter(Leg.class::isInstance) //
                .map(Leg.class::cast) //
                .filter(l -> l.getMode().equals(oldMode)) //
                .forEach(l -> setLegMode(l, newMode));
    }

    public static void removeRouteForLegs(List<? extends PlanElement> elements, String mode) {
        elements.stream() //
                .filter(Leg.class::isInstance) //
                .map(Leg.class::cast) //
                .filter(l -> l.getMode().equals(mode)) //
                .forEach(l -> l.setRoute(null));
    }

    public static void replaceTypeForActivities(List<? extends PlanElement> elements, String oldType, String newType) {
        elements.stream() //
                .filter(Activity.class::isInstance) //
                .map(Activity.class::cast) //
                .filter(l -> l.getType().equals(oldType)) //
                .forEach(l -> l.setType(newType));
    }

    public static void addMissingCoordsToPlanElementsFromLinks(Population population, Network network) {
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                List<Activity> activities = SalabimUtil.getActivities(plan.getPlanElements());
                SalabimUtil.addMissingCoordToActivitiesFromLink(activities, network);
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

    /**
     * @throws IllegalArgumentException in case elements are not alternating, i.e.
     *                                  an {@link Activity} is always followed by a
     *                                  {@link Leg} and vice versa
     */
    public static void assertAlternation(List<? extends PlanElement> elements) {
        for (int i = 0; i < elements.size() - 1; i++) {
            if (elements.get(i).getClass().equals(elements.get(i + 1).getClass())) {
                throw new IllegalArgumentException(String.format("elements %d and %d have the same type: %s", i,
                        (i + 1), elements.get(i).getClass().getName()));
            }
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    public static void assertStartAndEndWithLeg(List<? extends PlanElement> elements) {
        assertStartAndEndWithClass(elements, Leg.class);
    }

    /**
     * @throws IllegalArgumentException
     */
    public static void assertStartAndEndWithActivity(List<? extends PlanElement> elements) {
        assertStartAndEndWithClass(elements, Activity.class);
    }

    /**
     * Typical MATSim population files do not set the start time of the first (home)
     * activity and the end time of the last (home) activity. All activity times
     * inbetween are defined.
     * <p>
     * This function checks enforces this convention.
     * 
     * @throws IllegalArgumentException
     */
    public static void assertDefinedStartAndEndTimeOfActivitiesExceptFirstAndLast(
            List<? extends PlanElement> elements) {
        List<Activity> activities = getActivities(elements);
        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            if (activity.getStartTime().isUndefined() && i > 0) {
                throw new IllegalArgumentException(
                        "all but the first activity must have a start time, but it is missing from " + activity);
            }
            if (activity.getEndTime().isUndefined() && i < activities.size() - 1) {
                throw new IllegalArgumentException(
                        "all but the last activity must have an end time, but it is missing from " + activity);
            }
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    public static void assertDefinedDepartureAndTravelTimeForLegs(List<? extends PlanElement> elements) {
        getLegs(elements).forEach(leg -> {
            if (leg.getDepartureTime().isUndefined()) {
                throw new IllegalArgumentException("undefined departure time for leg " + leg);
            }
            if (leg.getTravelTime().isUndefined()) {
                throw new IllegalArgumentException("undefined travel time for leg " + leg);
            }
        });
    }

    private static void assertStartAndEndWithClass(List<? extends PlanElement> elements, Class<?> clazz) {
        if (elements.isEmpty())
            return;

        PlanElement element = elements.get(0);
        if (!clazz.isInstance(element)) {
            throw new IllegalArgumentException("first element is not a " + clazz.getName() + ": " + element);
        }

        element = elements.get(elements.size() - 1);
        if (!clazz.isInstance(element)) {
            throw new IllegalArgumentException("last element is not a " + clazz.getName() + ": " + element);
        }
    }

    /**
     * @return a deep copy
     */
    public static Activity clone(Activity activity, PopulationFactory factory) {
        Activity clone = factory.createActivityFromCoord(activity.getType(), activity.getCoord());
        activity.getStartTime().ifDefinedOrElse(t -> clone.setStartTime(t), () -> clone.setStartTimeUndefined());
        activity.getEndTime().ifDefinedOrElse(t -> clone.setEndTime(t), () -> clone.setEndTimeUndefined());
        activity.getMaximumDuration().ifDefinedOrElse(t -> clone.setMaximumDuration(t),
                () -> clone.setMaximumDurationUndefined());
        clone.setLinkId(activity.getLinkId());
        clone.setFacilityId(activity.getFacilityId());
        activity.getAttributes().getAsMap().forEach((k, v) -> clone.getAttributes().putAttribute(k, v));
        return clone;
    }

    /**
     * @return a deep copy (including the route)
     */
    public static Leg clone(Leg leg, PopulationFactory factory) {
        Leg clone = factory.createLeg(leg.getMode());
        leg.getDepartureTime().ifDefinedOrElse(t -> clone.setDepartureTime(t), () -> clone.setDepartureTimeUndefined());
        leg.getTravelTime().ifDefinedOrElse(t -> clone.setTravelTime(t), () -> clone.setTravelTimeUndefined());
        if (leg.getRoute() != null) {
            Route route = leg.getRoute();
            Route clonedRoute = factory.getRouteFactories().createRoute(route.getClass(), route.getStartLinkId(),
                    route.getEndLinkId());
            route.getTravelTime().ifDefinedOrElse(t -> clonedRoute.setTravelTime(t),
                    () -> clonedRoute.setTravelTimeUndefined());
            clonedRoute.setDistance(route.getDistance());
            clonedRoute.setRouteDescription(route.getRouteDescription());
            clone.setRoute(clonedRoute);
            clone.setRoute(route);
        }
        leg.getAttributes().getAsMap().forEach((k, v) -> clone.getAttributes().putAttribute(k, v));
        return clone;
    }

    public static List<PlanElement> clone(List<PlanElement> elements, PopulationFactory factory) {
        return elements.stream().map(e -> {
            if (e instanceof Leg) {
                return clone((Leg) e, factory);
            } else if (e instanceof Activity) {
                return clone((Activity) e, factory);
            }
            return e;
        }).collect(Collectors.toList());
    }

    /**
     * Removes persons from the scenario (keeps the first N persons)
     * 
     * @return the number of removed persons
     */
    public static int enforceMaxPopulationSize(Population population, int maxPopulationSize) {
        if (population.getPersons().size() <= maxPopulationSize)
            return 0;

        List<Id<Person>> keys = new ArrayList<>(population.getPersons().keySet());
        List<Id<Person>> deleteKeys = keys.subList(maxPopulationSize, keys.size());
        deleteKeys.forEach(k -> population.removePerson(k));
        return deleteKeys.size();
    }

    /**
     * Wraps the call to {@link Leg#setMode(String)} and avoids overwriting the
     * routingMode with null, which is a nasty side effect of the original method
     * 
     * @param leg
     * @param mode
     */
    public static void setLegMode(Leg leg, String mode) {
        String routingMode = TripStructureUtils.getRoutingMode(leg);
        leg.setMode(mode);
        TripStructureUtils.setRoutingMode(leg, routingMode);
    }

    /**
     * @return <code>true</code> iff all legs have the same non-null routingMode
     */
    public static boolean hasConsistentRoutingMode(Trip trip) {
        Set<Object> routingModes = new HashSet<>();
        for (Leg leg : trip.getLegsOnly()) {
            Object mode = leg.getAttributes().getAttribute("routingMode");
            if (mode == null) {
                return false;
            }
            routingModes.add(mode);
        }
        return routingModes.size() == 1;
    }

    public static boolean hasRoutingMode(Leg leg) {
        return leg.getAttributes().getAttribute("routingMode") != null;
    }

}
