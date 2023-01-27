package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.network.Node;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.core.router.util.LeastCostPathCalculator;
import java.util.HashMap;
import java.util.List;

public class RequestsFilter {
    private final CarpoolingConfigGroup cfgGroup;
    private final LeastCostPathCalculator router;

    public RequestsFilter(CarpoolingConfigGroup cfgGroup, LeastCostPathCalculator router) {
        this.cfgGroup = cfgGroup;
        this.router = router;
    }

    public HashMap<CarpoolingRequest, LeastCostPathCalculator.Path>  filterRequests(CarpoolingRequest driverRequest, List<CarpoolingRequest> passengersRequests) {
        HashMap<CarpoolingRequest, LeastCostPathCalculator.Path> filteredPassengerRequests = new HashMap<>();
        Node driverOrigin = driverRequest.getFromLink().getFromNode();
        double driverDepartureTime = driverRequest.getDepartureTime();
        for (CarpoolingRequest passengersRequest : passengersRequests) {
            Node passengerOrigin = passengersRequest.getFromLink().getFromNode();
            double passengerDepartureTime = passengersRequest.getDepartureTime();
            LeastCostPathCalculator.Path pathToPassenger = router.calcLeastCostPath(driverOrigin,
                    passengerOrigin, 0, null, null);
            double expectedPickupTime = driverDepartureTime+pathToPassenger.travelTime;
            boolean withinPassengerDepartureTimeWindow = (passengerDepartureTime-cfgGroup.passengerDepartureTimeAdjustment) < expectedPickupTime && expectedPickupTime < (passengerDepartureTime+cfgGroup.passengerDepartureTimeAdjustment);
            if (withinPassengerDepartureTimeWindow) {
                filteredPassengerRequests.put(passengersRequest,pathToPassenger);
            }
        }
        return filteredPassengerRequests;
    }
}
