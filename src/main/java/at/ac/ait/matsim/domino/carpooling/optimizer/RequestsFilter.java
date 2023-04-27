package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RequestsFilter {
    private final CarpoolingConfigGroup cfgGroup;
    private final RoutingModule router;

    public RequestsFilter(CarpoolingConfigGroup cfgGroup, RoutingModule router) {
        this.cfgGroup = cfgGroup;
        this.router = router;
    }

    public List<CarpoolingRequest> filterRequests(CarpoolingRequest driverRequest,
            List<CarpoolingRequest> ridersRequests) {
        List<CarpoolingRequest> filteredRiderRequests = new ArrayList<>();
        for (CarpoolingRequest riderRequest : ridersRequests) {
            Leg legToRider = CarpoolingUtil.calculateLeg(router,
                    driverRequest.getFromLink(),
                    riderRequest.getFromLink(),
                    driverRequest.getDepartureTime(),
                    driverRequest.getPerson());
            double expectedPickupTime = driverRequest.getDepartureTime() + legToRider.getTravelTime().seconds();
            if (checkRiderTimeConstraints(riderRequest, expectedPickupTime,
                    cfgGroup.getRiderDepartureTimeAdjustmentSeconds())) {
                filteredRiderRequests.add(riderRequest);
            }
        }
        return filteredRiderRequests;
    }

    static boolean checkRiderTimeConstraints(CarpoolingRequest riderRequest, double expectedPickupTime,
            double allowedTimeAdjustment) {
        double timeAdjustment = expectedPickupTime - riderRequest.getDepartureTime();
        boolean withinRiderDepartureTimeWindow = Math.abs(timeAdjustment) <= allowedTimeAdjustment;
        boolean constrainsPassed = false;
        if (withinRiderDepartureTimeWindow) {
            double timeBetweenStartAndEndAct = riderRequest.getTrip().getDestinationActivity().getStartTime().seconds()
                    - riderRequest.getTrip().getOriginActivity().getEndTime().seconds();
            if (timeAdjustment >= 0) {
                constrainsPassed = timeBetweenStartAndEndAct > timeAdjustment;
            } else if (timeAdjustment < 0) {
                int indexOfCurrentTrip = TripStructureUtils.getTrips(riderRequest.getPerson().getSelectedPlan())
                        .indexOf(riderRequest.getTrip());
                if (indexOfCurrentTrip == 0) {
                    constrainsPassed = true;
                } else {
                    double previousActEndTime = TripStructureUtils.getTrips(riderRequest.getPerson().getSelectedPlan())
                            .get(indexOfCurrentTrip - 1).getOriginActivity().getEndTime().seconds();
                    double timeBetweenStartAndPreviousAct = riderRequest.getTrip().getOriginActivity().getStartTime()
                            .seconds() - previousActEndTime;
                    constrainsPassed = timeBetweenStartAndPreviousAct > Math.abs(timeAdjustment);
                }
            }
        }
        return constrainsPassed;
    }
}