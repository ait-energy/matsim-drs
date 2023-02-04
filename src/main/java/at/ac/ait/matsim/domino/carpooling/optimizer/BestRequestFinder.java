package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;

import at.ac.ait.matsim.domino.carpooling.util.DominoUtil;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

public class BestRequestFinder {
    private final RoutingModule router;
    private final CarpoolingConfigGroup cfgGroup;
    public BestRequestFinder(RoutingModule router, CarpoolingConfigGroup cfgGroup) {
        this.router = router;
        this.cfgGroup = cfgGroup;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest, List<CarpoolingRequest> filteredPassengersRequests) {
        Map<CarpoolingRequest, Double> bestRequests = new HashMap<>();
        double originalRouteTravelTime = getLeg(driverRequest.getFromLink(), driverRequest.getToLink(), driverRequest.getDepartureTime(), router, driverRequest.getPerson()).getTravelTime().seconds();
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests){
            Leg legToCustomer = getLeg(driverRequest.getFromLink(), passengerRequest.getFromLink(),driverRequest.getDepartureTime(), router, driverRequest.getPerson());
            double travelTimeToCustomer = legToCustomer.getTravelTime().seconds();

            Leg legWithCustomer = getLeg(passengerRequest.getFromLink(), passengerRequest.getToLink(),driverRequest.getDepartureTime()+travelTimeToCustomer, router, driverRequest.getPerson());
            double travelTimeWithCustomer = legWithCustomer.getTravelTime().seconds();

            Leg legAfterCustomer = getLeg(passengerRequest.getToLink(), driverRequest.getToLink(), driverRequest.getDepartureTime()+travelTimeToCustomer+travelTimeWithCustomer, router, driverRequest.getPerson());
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
    private static Leg getLeg(Link fromLink, Link toLink, double departureTime, RoutingModule router, Person driver) {
        RoutingRequest routingRequest = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(fromLink),FacilitiesUtils.wrapLink(toLink), departureTime, driver);
        List<? extends PlanElement> legList = router.calcRoute(routingRequest);
        return DominoUtil.getFirstLeg(legList);
    }
}
