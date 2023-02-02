package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CarpoolingOptimizer {
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final NearestRequestsFinder nearestRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;

    public CarpoolingOptimizer(RequestsCollector requestsCollector, RequestsRegister requestsRegister, NearestRequestsFinder nearestRequestsFinder, RequestsFilter requestsFilter, BestRequestFinder bestRequestFinder) {
        this.requestsCollector = requestsCollector;
        this.requestsRegister = requestsRegister;
        this.nearestRequestsFinder = nearestRequestsFinder;
        this.requestsFilter = requestsFilter;
        this.bestRequestFinder = bestRequestFinder;
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> match() {
        HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        Collections.shuffle(driversRequests);
        List<CarpoolingRequest> passengersRequests = requestsCollector.getPassengersRequests();
        Collections.shuffle(passengersRequests);
        for (CarpoolingRequest passengersRequest : passengersRequests) {
            requestsRegister.addRequest(passengersRequest);
        }
        for (int i = 0; i < driversRequests.size(); i++) {
            List<CarpoolingRequest> nearestRequests = nearestRequestsFinder.findRegistryIntersections(driversRequests.get(i).getFromLink().getFromNode(),driversRequests.get(i).getToLink().getFromNode(),driversRequests.get(i).getDepartureTime());
            HashMap<CarpoolingRequest, List<? extends PlanElement >> filteredPassengersRequests = requestsFilter.filterRequests(driversRequests.get(i),nearestRequests);
            CarpoolingRequest bestPassengerRequest = bestRequestFinder.findBestRequest(driversRequests.get(i), filteredPassengersRequests);
            if (!(bestPassengerRequest == null)) {
                matchedRequests.put(driversRequests.get(i), bestPassengerRequest);
                driversRequests.remove(driversRequests.get(i));
                requestsRegister.removeRequest(bestPassengerRequest);
            }
        }
        return matchedRequests;
    }
}
