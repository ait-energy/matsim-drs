package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import java.util.ArrayList;
import java.util.HashMap;

public class CarpoolingOptimizer {
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;
    private final RequestsCollector requestsCollector;

    public CarpoolingOptimizer(RequestsCollector requestsCollector,  RequestsFilter requestsFilter,BestRequestFinder bestRequestFinder
                               ) {
        this.bestRequestFinder = bestRequestFinder;
        this.requestsFilter = requestsFilter;
        this.requestsCollector = requestsCollector;
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> match() {
        HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
        requestsCollector.collectRequests();
        ArrayList<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        ArrayList<CarpoolingRequest> passengersRequests = requestsCollector.getPassengersRequests();
        for (int i = 0; i < driversRequests.size(); i++) {
            ArrayList<CarpoolingRequest> filteredPassengersRequests = requestsFilter.filterRequests(driversRequests.get(i),passengersRequests);
            CarpoolingRequest bestPassengerRequest = bestRequestFinder.findBestRequest(driversRequests.get(i),
                    filteredPassengersRequests);
            if (!(bestPassengerRequest == null)) {
                matchedRequests.put(driversRequests.get(i), bestPassengerRequest);
                driversRequests.remove(driversRequests.get(i));
                passengersRequests.remove(bestPassengerRequest);
            }
        }
        return matchedRequests;
    }
}
