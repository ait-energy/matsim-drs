
package at.ac.ait.matsim.domino.carpooling.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * Based on
 * {@link org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour}
 */
public final class ChooseRandomLegModeForSubtourForCarpooling implements PlanAlgorithm {

    private static final Logger logger = Logger.getLogger(ChooseRandomLegModeForSubtourForCarpooling.class);

    private static class Candidate {
        final Subtour subtour;
        final String newTransportMode;

        public Candidate(
                final Subtour subtour,
                final String newTransportMode) {
            this.subtour = subtour;
            this.newTransportMode = newTransportMode;
        }
    }

    private Collection<String> modes;
    private final Collection<String> chainBasedModes;
    private Collection<String> singleTripSubtourModes;

    private final MainModeIdentifier mainModeIdentifier;

    private final Random rng;

    private PermissibleModesCalculator permissibleModesCalculator;

    public ChooseRandomLegModeForSubtourForCarpooling(
            final MainModeIdentifier mainModeIdentifier,
            final PermissibleModesCalculator permissibleModesCalculator,
            final String[] modes,
            final String[] chainBasedModes,
            final Random rng) {
        this.mainModeIdentifier = mainModeIdentifier;
        this.permissibleModesCalculator = permissibleModesCalculator;
        this.modes = Arrays.asList(modes);
        this.chainBasedModes = Arrays.asList(chainBasedModes);
        this.singleTripSubtourModes = this.chainBasedModes;

        this.rng = rng;
        logger.info("Chain based modes: " + this.chainBasedModes.toString());
    }

    /**
     * Some subtour consist of only a single trip, e.g. if a trip starts and ends on
     * the same link or facility.
     * By default, for those modes, the normal chain based modes are available. But
     * in certain cases, not all
     * the modes should be available for such trips (e.g. car-sharing does not make
     * much sense for such a trip),
     * thus the list of modes available for single-trip subtours can be specified
     * independently. As mentioned,
     * it is initialized by the constructor to the full list of chain based modes.
     * 
     * @param singleTripSubtourModes
     */
    public void setSingleTripSubtourModes(final String[] singleTripSubtourModes) {
        this.singleTripSubtourModes = Arrays.asList(singleTripSubtourModes);
    }

    @Override
    public void run(final Plan plan) {
        if (plan.getPlanElements().size() <= 1) {
            return;
        }

        final Id<? extends BasicLocation> homeLocation = ((Activity) plan.getPlanElements().get(0))
                .getFacilityId() != null ? ((Activity) plan.getPlanElements().get(0)).getFacilityId()
                        : ((Activity) plan.getPlanElements().get(0)).getLinkId();

        Collection<String> permissibleModesForThisPlan = permissibleModesCalculator.getPermissibleModes(plan);
        // (modes that agent can in principle use; e.g. cannot use car sharing if not
        // member)

        List<Candidate> choiceSet = determineChoiceSet(
                homeLocation,
                TripStructureUtils.getTrips(plan),
                TripStructureUtils.getSubtours(plan),
                permissibleModesForThisPlan);

        if (!choiceSet.isEmpty()) {
            Candidate whatToDo = choiceSet.get(rng.nextInt(choiceSet.size()));
            // (means that in the end we are changing modes only for one subtour)

            applyChange(whatToDo, plan);
        }
    }

    private List<Candidate> determineChoiceSet(
            final Id<? extends BasicLocation> homeLocation,
            final List<Trip> trips,
            final Collection<Subtour> subtours,
            final Collection<String> permissibleModesForThisPerson) {
        final ArrayList<Candidate> choiceSet = new ArrayList<Candidate>();
        for (Subtour subtour : subtours) {
            if (!subtour.isClosed()) {
                continue;
            }

            if (containsUnknownMode(subtour)) {
                continue;
            }

            final Id<? extends BasicLocation> subtourStartLocation =
                    // anchorAtFacilities ?
                    subtour.getTrips().get(0).getOriginActivity().getFacilityId() != null
                            ? subtour.getTrips().get(0).getOriginActivity().getFacilityId()
                            : subtour.getTrips().get(0).getOriginActivity().getLinkId();

            final Collection<String> testingModes = subtour.getTrips().size() == 1 ? singleTripSubtourModes
                    : chainBasedModes;
            // I am not sure what the singleTripSubtourModes thing means. But apart from
            // that ...

            // ... test whether a vehicle was brought to the subtourStartLocation:
            final Set<String> usableChainBasedModes = new LinkedHashSet<>();
            for (String mode : testingModes) {
                Id<? extends BasicLocation> vehicleLocation = homeLocation;
                Activity lastDestination = findLastDestinationOfMode(
                        trips.subList(
                                0,
                                trips.indexOf(subtour.getTrips().get(0))),
                        mode);
                if (lastDestination != null) {
                    vehicleLocation = getLocationId(lastDestination);
                }
                if (vehicleLocation.equals(subtourStartLocation)) {
                    usableChainBasedModes.add(mode);
                    // can have more than one mode here when subtour starts at home.
                }
            }
            // yy My intuition is that with the above condition, a switch of an all-car plan
            // with at least
            // one explicit sub-tour could switch to an all-bicycle plan only via first
            // changing the
            // explicit sub-tour first to a non-chain-based mode. kai, jul'18

            Set<String> usableModes = new LinkedHashSet<>();
            if (isMassConserving(subtour)) { // We can only replace a subtour if it doesn't itself move a vehicle from
                                             // one place to another
                for (String candidate : permissibleModesForThisPerson) {
                    if (chainBasedModes.contains(candidate)) {
                        // for chain-based modes, only add if vehicle is available:
                        if (usableChainBasedModes.contains(candidate)) {
                            usableModes.add(candidate);
                        }
                    } else {
                        // for non-chain-based modes, always add:
                        usableModes.add(candidate);
                    }
                }
            }

            usableModes.remove(getTransportMode(subtour));
            // (remove current mode so we don't get it again; note that the parent plan is
            // kept anyways)

            for (String transportMode : usableModes) {
                choiceSet.add(
                        new Candidate(
                                subtour,
                                transportMode));
            }
        }
        return choiceSet;
    }

    private boolean containsUnknownMode(final Subtour subtour) {
        for (Trip trip : subtour.getTrips()) {
            if (!modes.contains(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMassConserving(final Subtour subtour) {
        for (String mode : chainBasedModes) {
            if (!isMassConserving(subtour, mode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMassConserving(
            final Subtour subtour,
            final String mode) {
        final Activity firstOrigin = findFirstOriginOfMode(
                subtour.getTrips(),
                mode);

        if (firstOrigin == null) {
            return true;
        }

        final Activity lastDestination = findLastDestinationOfMode(
                subtour.getTrips(),
                mode);

        return atSameLocation(firstOrigin, lastDestination);
    }

    private Id<? extends BasicLocation> getLocationId(Activity activity) {
        return activity.getFacilityId() != null ? activity.getFacilityId() : activity.getLinkId();
    }

    private boolean atSameLocation(Activity firstLegUsingMode,
            Activity lastLegUsingMode) {
        return firstLegUsingMode.getFacilityId() != null ? firstLegUsingMode.getFacilityId().equals(
                lastLegUsingMode.getFacilityId())
                : firstLegUsingMode.getLinkId().equals(
                        lastLegUsingMode.getLinkId());
    }

    private Activity findLastDestinationOfMode(
            final List<Trip> tripsToSearch,
            final String mode) {
        final List<Trip> reversed = new ArrayList<>(tripsToSearch);
        Collections.reverse(reversed);
        for (Trip trip : reversed) {
            if (mode.equals(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return trip.getDestinationActivity();
            }
        }
        return null;
    }

    private Activity findFirstOriginOfMode(
            final List<Trip> tripsToSearch,
            final String mode) {
        for (Trip trip : tripsToSearch) {
            if (mode.equals(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return trip.getOriginActivity();
            }
        }
        return null;
    }

    private String getTransportMode(final Subtour subtour) {
        return mainModeIdentifier.identifyMainMode(
                subtour.getTrips().get(0).getTripElements());
    }

    private void applyChange(
            final Candidate whatToDo,
            final Plan plan) {
        for (Trip trip : whatToDo.subtour.getTrips()) {
            if (!modes.contains(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                // (ignore trips with modes that are not in "modes". MATSIM-809)
                continue;
            }

            TripRouter.insertTrip(
                    plan,
                    trip.getOriginActivity(),
                    Collections.singletonList(PopulationUtils.createLeg(whatToDo.newTransportMode)),
                    trip.getDestinationActivity());
        }

    }

}
