package at.ac.ait.matsim.drs.dmc;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;

import at.ac.ait.matsim.drs.run.Drs;

/**
 * TODO .. properly implement this class.
 * Right now it's just a copy of
 * https://github.com/eqasim-org/eqasim-java/blob/develop/core/src/main/java/org/eqasim/core/simulation/mode_choice/constraints/EqasimVehicleTourConstraint.java
 *
 *
 * Enforces the following scenario
 * 1) drsRider are picked up at meeting points only
 * 2) drsRider drives to the meeting point by car - at the beginning of the tour
 * 3) The first trip is therefore with the personal car to the meeting point and
 * then with the drsDriver to the destination
 * 4) consecutive trips can only use foot or pt, execpt for the last trip
 * 5) the last trip is via ridesharing back to the meeting point and from there
 * back with the personal car.
 *
 * Therefore, in case drsRider is present on any leg we check if the mode
 * sequence conforms to
 * drsRider - (pt|foot)* - drsRider
 *
 *
 * NOTE: where do we check if the tour starts and ends at the same location?
 */
public class DrsRiderWithInitialMeetingPointTourConstraint implements TourConstraint {
    public static final String NAME = "DrsRiderWithInitialMeetingPoint";

    private final Id<? extends BasicLocation> vehicleLocationId;

    public DrsRiderWithInitialMeetingPointTourConstraint(Id<? extends BasicLocation> vehicleLocationId) {
        this.vehicleLocationId = vehicleLocationId;
    }

    private int getFirstIndex(String mode, List<String> modes) {
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equals(mode)) {
                return i;
            }
        }

        return -1;
    }

    private int getLastIndex(String mode, List<String> modes) {
        for (int i = modes.size() - 1; i >= 0; i--) {
            if (modes.get(i).equals(mode)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
            List<List<String>> previousModes) {
        String restrictedMode = Drs.RIDER_MODE;
        if (!modes.contains(restrictedMode)) {
            return true;
        }
        // TODO implement our logic
        return true;
    }

    public boolean validateBeforeEstimation__example(List<DiscreteModeChoiceTrip> tour, List<String> modes,
            List<List<String>> previousModes) {
        String restrictedMode = Drs.RIDER_MODE;
        if (!modes.contains(restrictedMode)) {
            return true;
        }

        // I) Make sure vehicle is picked up and dropped off at its predetermined home
        // base. If the chain does not start at the vehicle base, the vehicle may also
        // be picked up at the first activity. If the chain does not end at the vehicle
        // base, the vehicle may still be dropped off at the last activity.

        int firstIndex = getFirstIndex(restrictedMode, modes);
        int lastIndex = getLastIndex(restrictedMode, modes);

        Id<? extends BasicLocation> startLocationId = LocationUtils
                .getLocationId(tour.get(firstIndex).getOriginActivity());
        Id<? extends BasicLocation> endLocationId = LocationUtils
                .getLocationId(tour.get(lastIndex).getDestinationActivity());

        if (!startLocationId.equals(vehicleLocationId)) {
            // Vehicle does not depart at the depot

            if (firstIndex > 0) {
                // If vehicle starts at very first activity, we still allow this tour!
                return false;
            }
        }

        if (!endLocationId.equals(vehicleLocationId)) {
            // Vehicle does not end at the depot

            if (lastIndex < modes.size() - 1) {
                // If vehicle ends at the very last activity, we still allow this tour!
                return false;
            }
        }

        // II) Make sure that in between the vehicle is only picked up at the location
        // where it has been moved previously

        Id<? extends BasicLocation> currentLocationId = LocationUtils
                .getLocationId(tour.get(firstIndex).getDestinationActivity());

        for (int index = firstIndex + 1; index <= lastIndex; index++) {
            if (modes.get(index).equals(restrictedMode)) {
                DiscreteModeChoiceTrip trip = tour.get(index);

                if (!currentLocationId.equals(LocationUtils.getLocationId(trip.getOriginActivity()))) {
                    return false;
                }

                currentLocationId = LocationUtils.getLocationId(trip.getDestinationActivity());
            }
        }

        return true;

    }

    @Override
    public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
            List<TourCandidate> previousCandidates) {
        return true;
    }

    public static class Factory implements TourConstraintFactory {
        private final HomeFinder homeFinder;

        public Factory(HomeFinder homeFinder) {
            this.homeFinder = homeFinder;
        }

        @Override
        public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                Collection<String> availableModes) {
            return new DrsRiderWithInitialMeetingPointTourConstraint(
                    homeFinder.getHomeLocationId(planTrips));
        }
    }
}