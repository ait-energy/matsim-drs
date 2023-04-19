package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.network.Link;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;

import java.util.ArrayList;
import java.util.List;

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
        Link driverOrigin = driverRequest.getFromLink();
        double driverDepartureTime = driverRequest.getDepartureTime();
        for (CarpoolingRequest riderRequest : ridersRequests) {
            RoutingRequest toCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverOrigin),
                    FacilitiesUtils.wrapLink(riderRequest.getFromLink()), driverDepartureTime,
                    driverRequest.getPerson());
            List<? extends PlanElement> legToCustomerList = router.calcRoute(toCustomer);
            Leg legToCustomer = CarpoolingUtil.getFirstLeg(legToCustomerList);
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
