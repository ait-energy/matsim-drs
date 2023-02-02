package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.facilities.FacilitiesUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BestRequestFinder {
    private final RoutingModule router;
    private final CarpoolingConfigGroup cfgGroup;
    Logger LOGGER = LogManager.getLogger();
    public BestRequestFinder(RoutingModule router, CarpoolingConfigGroup cfgGroup) {
        this.router = router;
        this.cfgGroup = cfgGroup;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest, HashMap<CarpoolingRequest, List<? extends PlanElement>> filteredPassengersRequests) {
        HashMap<CarpoolingRequest,Double> bestRequests = new HashMap<>();
        RoutingRequest toDestination = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverRequest.getFromLink()),FacilitiesUtils.wrapLink(driverRequest.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
        List<? extends PlanElement> originalRouteList = router.calcRoute(toDestination);
        if (originalRouteList.size()>1||!(originalRouteList.get(0) instanceof Leg)) {LOGGER.warn("Their should be only one leg in this route.");}
        Leg originalRoute= (Leg) originalRouteList.get(0);
        double originalRouteTravelTime = originalRoute.getTravelTime().seconds();
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests.keySet()){
            List<? extends PlanElement> legToCustomerList = filteredPassengersRequests.get(passengerRequest);
            if (legToCustomerList.size()>1||!(legToCustomerList.get(0) instanceof Leg)) {LOGGER.warn("Their should be only one leg in this route.");}
            Leg legToCustomer= (Leg) legToCustomerList.get(0);
            double travelTimeToCustomer = legToCustomer.getTravelTime().seconds();

            RoutingRequest withCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(passengerRequest.getFromLink()),FacilitiesUtils.wrapLink(passengerRequest.getToLink()), travelTimeToCustomer, driverRequest.getPerson());
            List<? extends PlanElement> legWithCustomerList = router.calcRoute(withCustomer);
            if (legWithCustomerList.size()>1||!(legWithCustomerList.get(0) instanceof Leg)) {LOGGER.warn("Their should be only one leg in this route.");}
            Leg legWithCustomer= (Leg) legWithCustomerList.get(0);
            double travelTimeWithCustomer = legWithCustomer.getTravelTime().seconds();

            RoutingRequest afterCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(passengerRequest.getFromLink()),FacilitiesUtils.wrapLink(passengerRequest.getToLink()), travelTimeToCustomer, driverRequest.getPerson());
            List<? extends PlanElement> legAfterCustomerList = router.calcRoute(afterCustomer);
            if (legAfterCustomerList.size()>1||!(legAfterCustomerList.get(0) instanceof Leg)) {LOGGER.warn("Their should be only one leg in this route.");}
            Leg legAfterCustomer= (Leg) legAfterCustomerList.get(0);
            double travelTimeAfterCustomer = legAfterCustomer.getTravelTime().seconds();

            double newRouteTravelTime = travelTimeToCustomer + travelTimeWithCustomer + travelTimeAfterCustomer;
            double detourFactor = newRouteTravelTime/originalRouteTravelTime;
            if (detourFactor<cfgGroup.maxDetourFactor){
                bestRequests.put(passengerRequest,detourFactor);
            }
        }
        return findRequestWithLeastDetour(bestRequests);
    }

    private static CarpoolingRequest findRequestWithLeastDetour(HashMap<CarpoolingRequest,Double> bestRequests) {
        if (!bestRequests.isEmpty()){
            return Collections.min(bestRequests.entrySet(), Map.Entry.comparingByValue()).getKey();
        }else {
            return null;
        }
    }
}
