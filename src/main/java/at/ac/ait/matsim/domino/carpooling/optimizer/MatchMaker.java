package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final PotentialRequestsFinder potentialRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;
    private final HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests;
    private List<CarpoolingRequest> driversRequests;
    private List<CarpoolingRequest> ridersRequests;
    private final List<CarpoolingRequest> unmatchedDriversRequests;
    private final List<CarpoolingRequest> unmatchedRidersRequests;

    public MatchMaker(RequestsCollector requestsCollector, RequestsRegister requestsRegister,
            PotentialRequestsFinder potentialRequestsFinder, RequestsFilter requestsFilter,
            BestRequestFinder bestRequestFinder, HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests,
            List<CarpoolingRequest> driversRequests, List<CarpoolingRequest> ridersRequests,
            List<CarpoolingRequest> unmatchedDriversRequests, List<CarpoolingRequest> unmatchedRidersRequests) {
        this.requestsCollector = requestsCollector;
        this.requestsRegister = requestsRegister;
        this.potentialRequestsFinder = potentialRequestsFinder;
        this.requestsFilter = requestsFilter;
        this.bestRequestFinder = bestRequestFinder;
        this.matchedRequests = matchedRequests;
        this.driversRequests = driversRequests;
        this.ridersRequests = ridersRequests;
        this.unmatchedDriversRequests = unmatchedDriversRequests;
        this.unmatchedRidersRequests = unmatchedRidersRequests;
    }

    public void match() {
        requestsCollector.collectRequests();
        LOGGER.info(requestsCollector.getDriversRequests().size() + " drivers requests and "
                + requestsCollector.getRidersRequests().size() + " riders requests were collected.");
        driversRequests = requestsCollector.getDriversRequests();
        Collections.shuffle(driversRequests);
        ridersRequests = requestsCollector.getRidersRequests();
        Collections.shuffle(ridersRequests);

        for (CarpoolingRequest ridersRequest : ridersRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        for (Iterator<CarpoolingRequest> iterator = driversRequests.iterator(); iterator.hasNext();) {
            CarpoolingRequest driverRequest = iterator.next();
            List<CarpoolingRequest> potentialRequests = potentialRequestsFinder.findRegistryIntersections(
                    driverRequest.getFromLink().getFromNode(), driverRequest.getToLink().getFromNode(),
                    driverRequest.getDepartureTime());
            List<CarpoolingRequest> filteredRidersRequests = requestsFilter.filterRequests(driverRequest,
                    potentialRequests);
            CarpoolingRequest bestRiderRequest = bestRequestFinder.findBestRequest(driverRequest,
                    filteredRidersRequests);

            if (!(bestRiderRequest == null)) {
                CarpoolingUtil.setRequestStatus(bestRiderRequest.getLeg(), "matched");
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
                iterator.remove();
                requestsRegister.removeRequest(bestRiderRequest);
            }
        }
        LOGGER.info(matchedRequests.size() + " matches happened. Matching process finished!");
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> getMatchedRequests() {
        return matchedRequests;
    }

    public List<CarpoolingRequest> getUnmatchedDriverRequests() {

        for (CarpoolingRequest request : driversRequests) {
            if (!matchedRequests.containsKey(request)) {
                unmatchedDriversRequests.add(request);
            }
        }
        return unmatchedDriversRequests;
    }

    public List<CarpoolingRequest> getUnmatchedRiderRequests() {

        for (CarpoolingRequest request : ridersRequests) {
            if (!matchedRequests.containsValue(request)) {
                unmatchedRidersRequests.add(request);
            }
        }
        return unmatchedRidersRequests;
    }
}
