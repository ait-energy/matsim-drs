package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.io.BufferedWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import at.ac.ait.matsim.domino.carpooling.analysis.StatsCollector;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MatchMaker {
    Logger LOGGER = LogManager.getLogger();
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final NearestRequestsFinder nearestRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;
    private final Integer iterationNumber;

    public MatchMaker(RequestsCollector requestsCollector, RequestsRegister requestsRegister, NearestRequestsFinder nearestRequestsFinder, RequestsFilter requestsFilter, BestRequestFinder bestRequestFinder, Integer iterationNumber) {
        this.requestsCollector = requestsCollector;
        this.requestsRegister = requestsRegister;
        this.nearestRequestsFinder = nearestRequestsFinder;
        this.requestsFilter = requestsFilter;
        this.bestRequestFinder = bestRequestFinder;
        this.iterationNumber = iterationNumber;
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> match() {
        HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
        requestsCollector.collectRequests();
        LOGGER.info(requestsCollector.getDriversRequests().size()+" drivers requests and "+requestsCollector.getRidersRequests().size()+" riders requests were collected.");
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        Collections.shuffle(driversRequests);
        List<CarpoolingRequest> ridersRequests = requestsCollector.getRidersRequests();
        Collections.shuffle(ridersRequests);

        for (CarpoolingRequest ridersRequest : ridersRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        BufferedWriter bufferedWriter1 = StatsCollector.createWriter("output/CarpoolingStats/ITERS/it."+iterationNumber+"/driverRequests.txt","Driver request,Person id,Departure time,OriginX,OriginY,DestinationX,DestinationY,Matched,Filtered requests,Nearest requests");
        for(Iterator<CarpoolingRequest> iterator = driversRequests.iterator(); iterator.hasNext(); ) {
            CarpoolingRequest driverRequest = iterator.next();
            List<CarpoolingRequest> nearestRequests = nearestRequestsFinder.findRegistryIntersections(driverRequest.getFromLink().getFromNode(),driverRequest.getToLink().getFromNode(),driverRequest.getDepartureTime());
            List<CarpoolingRequest> filteredRidersRequests = requestsFilter.filterRequests(driverRequest,nearestRequests);
            CarpoolingRequest bestRiderRequest = bestRequestFinder.findBestRequest(driverRequest, filteredRidersRequests);

            StatsCollector.collectDriversRequestsStats(bufferedWriter1, driverRequest,nearestRequests,filteredRidersRequests,bestRiderRequest);

            if (!(bestRiderRequest == null)) {
                CarpoolingUtil.setLinkageActivityToRiderRequest(bestRiderRequest);
                LOGGER.warn(driverRequest.getPerson().getId()+"'s best rider match is "+bestRiderRequest.getPerson().getId()+". Pickup point is "+bestRiderRequest.getFromLink().getId());
                matchedRequests.put(driverRequest, bestRiderRequest);
                driverRequest.setMatched();
                bestRiderRequest.setMatched();
                iterator.remove();
                requestsRegister.removeRequest(bestRiderRequest);
            }
        }
        BufferedWriter bufferedWriter2 = StatsCollector.createWriter("output/CarpoolingStats/ITERS/it."+iterationNumber+"/riderRequests.txt","Rider request,Person id,Departure time,OriginX,OriginY,DestinationX,DestinationY,Matched");
        StatsCollector.collectRidersRequestsStats(bufferedWriter2,ridersRequests);
        StatsCollector.close(bufferedWriter2);
        StatsCollector.close(bufferedWriter1);
        LOGGER.info(matchedRequests.size()+" matches happened.");
        LOGGER.info("Matching process finished!");
        return matchedRequests;
    }
}
