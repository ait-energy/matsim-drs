package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.network.Link;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class RequestsFilter {
    private final CarpoolingConfigGroup cfgGroup;
    private final RoutingModule router;
    Logger LOGGER = LogManager.getLogger();
    public RequestsFilter(CarpoolingConfigGroup cfgGroup, RoutingModule router) {
        this.cfgGroup = cfgGroup;
        this.router = router;
    }

    public HashMap<CarpoolingRequest, List<? extends PlanElement> >  filterRequests(CarpoolingRequest driverRequest, List<CarpoolingRequest> passengersRequests) {
        HashMap<CarpoolingRequest, List<? extends PlanElement> > filteredPassengerRequests = new HashMap<>();
        Link driverOrigin = driverRequest.getFromLink();
        double driverDepartureTime = driverRequest.getDepartureTime();
        for (CarpoolingRequest passengerRequest : passengersRequests) {
            RoutingRequest toCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverOrigin),FacilitiesUtils.wrapLink(passengerRequest.getFromLink()), driverDepartureTime, driverRequest.getPerson());
            List<? extends PlanElement> legToCustomerList = router.calcRoute(toCustomer);
            if (legToCustomerList.size()>1||!(legToCustomerList.get(0) instanceof Leg)) {LOGGER.warn("Their should be only one leg in this route.");}
            Leg legToCustomer= (Leg) legToCustomerList.get(0);
            double expectedPickupTime = driverRequest.getDepartureTime()+ legToCustomer.getTravelTime().seconds();
            boolean withinPassengerDepartureTimeWindow = (expectedPickupTime-cfgGroup.passengerDepartureTimeAdjustment) < expectedPickupTime && expectedPickupTime < (expectedPickupTime+cfgGroup.passengerDepartureTimeAdjustment);
            if (withinPassengerDepartureTimeWindow) {
                filteredPassengerRequests.put(passengerRequest,legToCustomerList);
            }
        }
        return filteredPassengerRequests;
    }
}
