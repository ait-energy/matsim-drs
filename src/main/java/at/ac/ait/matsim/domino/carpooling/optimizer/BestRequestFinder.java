package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class BestRequestFinder {
    private final RoutingModule router;

    public BestRequestFinder(RoutingModule router) {
        this.router = router;
    }

    /**
     * @return null if no match was found
     */
    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest,
            List<CarpoolingRequest> filteredRidersRequests) {
        Map<CarpoolingRequest, Double> bestRequests = new HashMap<>();
        double originalRouteTravelTime = CarpoolingUtil
                .calculateLeg(router, driverRequest.getFromLink(), driverRequest.getToLink(),
                        driverRequest.getDepartureTime(), driverRequest.getPerson())
                .getTravelTime().seconds();
        if (originalRouteTravelTime == 0) {
            return null;
        }

        for (CarpoolingRequest riderRequest : filteredRidersRequests) {
            Leg legToCustomer = CarpoolingUtil.calculateLeg(router, driverRequest.getFromLink(),
                    riderRequest.getFromLink(), driverRequest.getDepartureTime(), driverRequest.getPerson());
            double travelTimeToCustomer = legToCustomer.getTravelTime().seconds();

            Leg legWithCustomer = CarpoolingUtil.calculateLeg(router, riderRequest.getFromLink(),
                    riderRequest.getToLink(), driverRequest.getDepartureTime() + travelTimeToCustomer,
                    driverRequest.getPerson());
            double travelTimeWithCustomer = legWithCustomer.getTravelTime().seconds();

            Leg legAfterCustomer = CarpoolingUtil.calculateLeg(router, riderRequest.getToLink(),
                    driverRequest.getToLink(),
                    driverRequest.getDepartureTime() + travelTimeToCustomer + travelTimeWithCustomer,
                    driverRequest.getPerson());
            double travelTimeAfterCustomer = legAfterCustomer.getTravelTime().seconds();

            double newRouteTravelTime = travelTimeToCustomer + travelTimeWithCustomer + travelTimeAfterCustomer;
            double detourFactor = newRouteTravelTime / originalRouteTravelTime;
            riderRequest.setDetourFactor(detourFactor);
            bestRequests.put(riderRequest, detourFactor);
        }
        CarpoolingRequest bestRequest = CarpoolingUtil.findRequestWithLeastDetour(bestRequests);
        if (bestRequest != null) {
            driverRequest.setDetourFactor(bestRequest.getDetourFactor());
        }
        return bestRequest;
    }

}