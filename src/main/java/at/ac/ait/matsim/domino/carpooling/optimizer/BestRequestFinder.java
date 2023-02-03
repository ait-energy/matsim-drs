package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;

import at.ac.ait.matsim.domino.DominoUtil;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

public class BestRequestFinder {
    private final RoutingModule router;
    private final CarpoolingConfigGroup cfgGroup;
    Logger LOGGER = LogManager.getLogger();
    public BestRequestFinder(RoutingModule router, CarpoolingConfigGroup cfgGroup) {
        this.router = router;
        this.cfgGroup = cfgGroup;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest,
            Map<CarpoolingRequest, List<? extends PlanElement>> filteredPassengersRequests) {
        Map<CarpoolingRequest, Double> bestRequests = new HashMap<>();
        RoutingRequest toDestination = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverRequest.getFromLink()),FacilitiesUtils.wrapLink(driverRequest.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
        List<? extends PlanElement> originalRouteList = router.calcRoute(toDestination);
        Leg originalRoute = DominoUtil.getFirstLeg(originalRouteList);
        double originalRouteTravelTime = originalRoute.getTravelTime().seconds();
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests.keySet()){
            List<? extends PlanElement> legToCustomerList = filteredPassengersRequests.get(passengerRequest);
            Leg legToCustomer = DominoUtil.getFirstLeg(legToCustomerList);
            double travelTimeToCustomer = legToCustomer.getTravelTime().seconds();

            RoutingRequest withCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(passengerRequest.getFromLink()),FacilitiesUtils.wrapLink(passengerRequest.getToLink()), travelTimeToCustomer, driverRequest.getPerson());
            List<? extends PlanElement> legWithCustomerList = router.calcRoute(withCustomer);
            Leg legWithCustomer = DominoUtil.getFirstLeg(legWithCustomerList);
            double travelTimeWithCustomer = legWithCustomer.getTravelTime().seconds();

            RoutingRequest afterCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(passengerRequest.getFromLink()),FacilitiesUtils.wrapLink(passengerRequest.getToLink()), travelTimeToCustomer, driverRequest.getPerson());
            List<? extends PlanElement> legAfterCustomerList = router.calcRoute(afterCustomer);
            Leg legAfterCustomer = DominoUtil.getFirstLeg(legAfterCustomerList);
            double travelTimeAfterCustomer = legAfterCustomer.getTravelTime().seconds();

            double newRouteTravelTime = travelTimeToCustomer + travelTimeWithCustomer + travelTimeAfterCustomer;
            double detourFactor = newRouteTravelTime/originalRouteTravelTime;
            if (detourFactor<cfgGroup.maxDetourFactor){
                bestRequests.put(passengerRequest,detourFactor);
            }
        }
        return findRequestWithLeastDetour(bestRequests);
    }

    private static CarpoolingRequest findRequestWithLeastDetour(Map<CarpoolingRequest, Double> bestRequests) {
        if (!bestRequests.isEmpty()){
            return Collections.min(bestRequests.entrySet(), Map.Entry.comparingByValue()).getKey();
        }else {
            return null;
        }
    }
}
