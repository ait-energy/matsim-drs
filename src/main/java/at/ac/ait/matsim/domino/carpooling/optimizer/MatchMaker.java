package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.common.collect.Lists;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final PotentialRequestsFinder potentialRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;

    private List<CarpoolingRequest> driverRequests;
    private List<CarpoolingRequest> riderRequests;
    private Map<CarpoolingRequest, CarpoolingRequest> matchedRequests;
    private List<CarpoolingRequest> unmatchedDriverRequests;
    private List<CarpoolingRequest> unmatchedRiderRequests;

    public MatchMaker(RequestsCollector requestsCollector, RequestsRegister requestsRegister,
            PotentialRequestsFinder potentialRequestsFinder, RequestsFilter requestsFilter,
            BestRequestFinder bestRequestFinder) {
        this.requestsCollector = requestsCollector;
        this.requestsRegister = requestsRegister;
        this.potentialRequestsFinder = potentialRequestsFinder;
        this.requestsFilter = requestsFilter;
        this.bestRequestFinder = bestRequestFinder;
    }

    public void match() {
        requestsCollector.collectRequests();
        driverRequests = Lists.newArrayList(requestsCollector.getDriverRequests());
        Collections.shuffle(driverRequests);
        riderRequests = Lists.newArrayList(requestsCollector.getRiderRequests());
        Collections.shuffle(riderRequests);

        for (CarpoolingRequest ridersRequest : riderRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        matchedRequests = new HashMap<>();
        for (Iterator<CarpoolingRequest> iterator = driverRequests.iterator(); iterator.hasNext();) {
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

        unmatchedDriverRequests = new ArrayList<>();
        for (CarpoolingRequest request : driverRequests) {
            if (!matchedRequests.containsKey(request)) {
                unmatchedDriverRequests.add(request);
            }
        }

        unmatchedRiderRequests = new ArrayList<>();
        for (CarpoolingRequest request : riderRequests) {
            if (!matchedRequests.containsValue(request)) {
                unmatchedRiderRequests.add(request);
            }
        }

        LOGGER.info(matchedRequests.size() + " matches happened. Matching process finished!");
    }

    public List<CarpoolingRequest> getDriverRequests() {
        return Collections.unmodifiableList(driverRequests);
    }

    public List<CarpoolingRequest> getRiderRequests() {
        return Collections.unmodifiableList(riderRequests);
    }

    public Map<CarpoolingRequest, CarpoolingRequest> getMatchedRequests() {
        return Collections.unmodifiableMap(matchedRequests);
    }

    public List<CarpoolingRequest> getUnmatchedDriverRequests() {
        return Collections.unmodifiableList(unmatchedDriverRequests);
    }

    public List<CarpoolingRequest> getUnmatchedRiderRequests() {
        return Collections.unmodifiableList(unmatchedRiderRequests);
    }

}
