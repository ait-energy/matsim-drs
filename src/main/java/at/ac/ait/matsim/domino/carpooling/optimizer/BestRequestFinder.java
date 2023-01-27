package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BestRequestFinder {
    private final LeastCostPathCalculator router;
    private final CarpoolingConfigGroup cfgGroup;
    public BestRequestFinder(LeastCostPathCalculator router, CarpoolingConfigGroup cfgGroup) {
        this.router = router;
        this.cfgGroup = cfgGroup;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest, HashMap<CarpoolingRequest, LeastCostPathCalculator.Path> filteredPassengersRequests) {
        HashMap<CarpoolingRequest,Double> bestRequests = new HashMap<>();
        Node driverOrigin = driverRequest.getFromLink().getFromNode();
        Node driverDestination = driverRequest.getToLink().getFromNode();
        LeastCostPathCalculator.Path originalPath = router.calcLeastCostPath(driverOrigin,
                driverDestination, 0, null, null);
        double originalPathTravelTime = originalPath.travelTime;
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests.keySet()){
            Node passengerOrigin = passengerRequest.getFromLink().getFromNode();
            Node passengerDestination = passengerRequest.getToLink().getFromNode();
            LeastCostPathCalculator.Path pathToCustomer = filteredPassengersRequests.get(passengerRequest);
            double travelTimeToCustomer = pathToCustomer.travelTime;
            LeastCostPathCalculator.Path pathWithCustomer = router.calcLeastCostPath(passengerOrigin,
                    passengerDestination, 0, null, null);
            double travelTimeWithCustomer = pathWithCustomer.travelTime;
            LeastCostPathCalculator.Path pathAfterCustomer = router
                    .calcLeastCostPath(passengerDestination, driverDestination, 0, null, null);
            double travelTimeAfterCustomer = pathAfterCustomer.travelTime;
            double newPathTravelTime = travelTimeToCustomer + travelTimeWithCustomer + travelTimeAfterCustomer;
            double detourFactor = newPathTravelTime/originalPathTravelTime;
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
