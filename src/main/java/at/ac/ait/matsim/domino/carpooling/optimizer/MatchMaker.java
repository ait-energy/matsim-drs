package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.io.BufferedWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import at.ac.ait.matsim.domino.carpooling.analysis.StatsCollector;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();

    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final NearestRequestsFinder nearestRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;
    private final Integer iteration;
    private final OutputDirectoryHierarchy output;

    public MatchMaker(RequestsCollector requestsCollector, RequestsRegister requestsRegister,
            NearestRequestsFinder nearestRequestsFinder, RequestsFilter requestsFilter,
            BestRequestFinder bestRequestFinder, Integer iteration, OutputDirectoryHierarchy output) {
        this.requestsCollector = requestsCollector;
        this.requestsRegister = requestsRegister;
        this.nearestRequestsFinder = nearestRequestsFinder;
        this.requestsFilter = requestsFilter;
        this.bestRequestFinder = bestRequestFinder;
        this.iteration = iteration;
        this.output = output;
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> match() {
        HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
        requestsCollector.collectRequests();
        LOGGER.info(requestsCollector.getDriversRequests().size() + " drivers requests and "
                + requestsCollector.getRidersRequests().size() + " riders requests were collected.");
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        Collections.shuffle(driversRequests);
        List<CarpoolingRequest> ridersRequests = requestsCollector.getRidersRequests();
        Collections.shuffle(ridersRequests);

        for (CarpoolingRequest ridersRequest : ridersRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        BufferedWriter driverWriter = StatsCollector.createWriter(
                output.getIterationFilename(iteration, "carpooling_driverRequests.txt"),
                "Driver request,Person id,Departure time,OriginX,OriginY,DestinationX,DestinationY,Matched,Filtered requests,Nearest requests");
        for (Iterator<CarpoolingRequest> iterator = driversRequests.iterator(); iterator.hasNext();) {
            CarpoolingRequest driverRequest = iterator.next();
            List<CarpoolingRequest> nearestRequests = nearestRequestsFinder.findRegistryIntersections(
                    driverRequest.getFromLink().getFromNode(), driverRequest.getToLink().getFromNode(),
                    driverRequest.getDepartureTime());
            List<CarpoolingRequest> filteredRidersRequests = requestsFilter.filterRequests(driverRequest,
                    nearestRequests);
            CarpoolingRequest bestRiderRequest = bestRequestFinder.findBestRequest(driverRequest,
                    filteredRidersRequests);

            StatsCollector.collectDriversRequestsStats(driverWriter, driverRequest, nearestRequests,
                    filteredRidersRequests, bestRiderRequest);

            if (!(bestRiderRequest == null)) {
                for (PlanElement planElement : bestRiderRequest.getPerson().getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity) {
                        if (((Activity) planElement).getEndTime().isDefined()) {
                            if (((Activity) planElement).getEndTime().seconds() == bestRiderRequest
                                    .getDepartureTime()) {
                                CarpoolingUtil.setLinkageActivityToRiderRequest((Activity) planElement,
                                        bestRiderRequest.getId().toString());
                                break;
                            }
                        }
                    }
                }

                LOGGER.info(driverRequest.getPerson().getId() + "'s best rider match is "
                        + bestRiderRequest.getPerson().getId() + ". Pickup point is "
                        + bestRiderRequest.getFromLink().getId());
                matchedRequests.put(driverRequest, bestRiderRequest);
                driverRequest.setMatched();
                bestRiderRequest.setMatched();
                iterator.remove();
                requestsRegister.removeRequest(bestRiderRequest);
            }
        }
        BufferedWriter riderWriter = StatsCollector.createWriter(
                output.getIterationFilename(iteration, "carpooling_riderRequests.txt"),
                "Rider request,Person id,Departure time,OriginX,OriginY,DestinationX,DestinationY,Matched");
        StatsCollector.collectRidersRequestsStats(riderWriter, ridersRequests);
        StatsCollector.close(riderWriter);
        StatsCollector.close(driverWriter);
        LOGGER.info(matchedRequests.size() + " matches happened.");
        LOGGER.info("Matching process finished!");
        return matchedRequests;
    }
}
