package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.RoutingModule;

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
            Leg legToCustomer = CarpoolingUtil.calculateLeg(router,
                    driverRequest.getFromLink(),
                    riderRequest.getFromLink(),
                    driverRequest.getDepartureTime(),
                    driverRequest.getPerson());
            double expectedPickupTime = driverRequest.getDepartureTime() + legToCustomer.getTravelTime().seconds();
            boolean withinRiderDepartureTimeWindow = (riderRequest.getDepartureTime()
                    - cfgGroup.getRiderDepartureTimeAdjustmentSeconds()) < expectedPickupTime
                    && expectedPickupTime < (riderRequest.getDepartureTime()
                            + cfgGroup.getRiderDepartureTimeAdjustmentSeconds());
            if (withinRiderDepartureTimeWindow) {
                filteredRiderRequests.add(riderRequest);
            }
        }
        return filteredRiderRequests;
    }
}
