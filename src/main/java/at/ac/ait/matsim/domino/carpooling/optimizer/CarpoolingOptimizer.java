package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarpoolingOptimizer {

    Logger LOGGER = LogManager.getLogger();
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
        List<CarpoolingRequest> ridersRequests = requestsCollector.getRidersRequests();
        Collections.shuffle(ridersRequests);
        for (CarpoolingRequest ridersRequest : ridersRequests) {
            requestsRegister.addRequest(ridersRequest);
        }
        for(Iterator<CarpoolingRequest> iterator = driversRequests.iterator(); iterator.hasNext(); ) {
            CarpoolingRequest driverRequest = iterator.next();
            List<CarpoolingRequest> nearestRequests = nearestRequestsFinder.findRegistryIntersections(driverRequest.getFromLink().getFromNode(),driverRequest.getToLink().getFromNode(),driverRequest.getDepartureTime());
            LOGGER.error(driverRequest.getPerson().getId()+" had "+nearestRequests.size()+" near requests.");
            List<CarpoolingRequest> filteredRidersRequests = requestsFilter.filterRequests(driverRequest,nearestRequests);
            LOGGER.error(driverRequest.getPerson().getId()+" had "+filteredRidersRequests.size()+" filtered requests.");
            CarpoolingRequest bestRiderRequest = bestRequestFinder.findBestRequest(driverRequest, filteredRidersRequests);
            if (!(bestRiderRequest == null)) {
                LOGGER.error(driverRequest.getPerson().getId()+"'s best rider match is "+bestRiderRequest.getPerson().getId()+" pickup point is "+bestRiderRequest.getFromLink().getId());
                matchedRequests.put(driverRequest, bestRiderRequest);
                iterator.remove();
                requestsRegister.removeRequest(bestRiderRequest);
            }
        }
        return matchedRequests;
    }
}
