package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class PotentialMatchFinder {
    private final DrsConfigGroup drsConfig;
    private final RoutingModule router;

    public PotentialMatchFinder(DrsConfigGroup drsConfig, RoutingModule router) {
        this.drsConfig = drsConfig;
        this.router = router;
    }

    /**
     * @return matches for rider requests that meet the filter criteria
     */
    public List<DrsMatch> filterRequests(DrsDriverRequest driverRequest,
            List<DrsRiderRequest> ridersRequests) {
        List<DrsMatch> filteredRiderRequests = new ArrayList<>();
        for (DrsRiderRequest riderRequest : ridersRequests) {
            Leg toPickup = DrsUtil.calculateLeg(router,
                    driverRequest.getFromLink(),
                    riderRequest.getFromLink(),
                    driverRequest.getDepartureTime(),
                    driverRequest.getPerson());
            double expectedPickupTime = driverRequest.getDepartureTime() + toPickup.getTravelTime().seconds();
            if (checkRiderTimeConstraints(riderRequest, expectedPickupTime,
                    drsConfig.getRiderDepartureTimeAdjustmentSeconds())) {
                filteredRiderRequests.add(DrsMatch.createMinimal(driverRequest, riderRequest, toPickup));
            }
        }
        return filteredRiderRequests;
    }

    static boolean checkRiderTimeConstraints(DrsRiderRequest riderRequest, double expectedPickupTime,
            double allowedTimeAdjustment) {
        double timeAdjustment = expectedPickupTime - riderRequest.getDepartureTime();
        boolean withinRiderDepartureTimeWindow = Math.abs(timeAdjustment) <= allowedTimeAdjustment;
        boolean constraintsPassed = false;
        if (withinRiderDepartureTimeWindow) {
            if (timeAdjustment >= 0) {
                constraintsPassed = true;
                // TODO rework this logic. Maybe check next
                // activities' end instead of start I would propose.
                double duration = riderRequest.getTrip().getDestinationActivity().getStartTime().seconds() -
                        riderRequest.getTrip().getOriginActivity().getEndTime().seconds();
                constraintsPassed = duration > timeAdjustment;
            } else if (timeAdjustment < 0) {
                int indexOfCurrentTrip = TripStructureUtils.getTrips(riderRequest.getPerson().getSelectedPlan())
                        .indexOf(riderRequest.getTrip());
                if (indexOfCurrentTrip == 0) {
                    constraintsPassed = true;
                } else {
                    double previousActEndTime = TripStructureUtils.getTrips(riderRequest.getPerson().getSelectedPlan())
                            .get(indexOfCurrentTrip - 1).getOriginActivity().getEndTime().seconds();
                    double timeBetweenStartAndPreviousAct = riderRequest.getTrip().getOriginActivity().getStartTime()
                            .seconds() - previousActEndTime;
                    constraintsPassed = timeBetweenStartAndPreviousAct > Math.abs(timeAdjustment);
                }
            }
        }
        return constraintsPassed;
    }
}