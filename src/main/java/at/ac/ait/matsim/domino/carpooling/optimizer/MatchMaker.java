package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.common.collect.Lists;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingMatch;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final PotentialRequestsFinder potentialRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;

    private List<CarpoolingMatch> matches;
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
        if (matches != null) {
            throw new RuntimeException("can only match once");
        }

        requestsCollector.collectRequests();
        List<CarpoolingRequest> driverRequests = Lists.newArrayList(requestsCollector.getDriverRequests());
        Collections.shuffle(driverRequests);
        List<CarpoolingRequest> riderRequests = Lists.newArrayList(requestsCollector.getRiderRequests());
        Collections.shuffle(riderRequests);

        for (CarpoolingRequest ridersRequest : riderRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        matches = new ArrayList<>();
        for (Iterator<CarpoolingRequest> iterator = driverRequests.iterator(); iterator.hasNext();) {
            CarpoolingRequest driverRequest = iterator.next();
            List<CarpoolingRequest> potentialRequests = potentialRequestsFinder.findRegistryIntersections(
                    driverRequest.getFromLink().getFromNode(), driverRequest.getToLink().getFromNode(),
                    driverRequest.getDepartureTime());
            List<CarpoolingMatch> filteredMatches = requestsFilter.filterRequests(driverRequest, potentialRequests);
            CarpoolingMatch bestMatch = bestRequestFinder.findBestRequest(filteredMatches);

            if (bestMatch == null) {
                continue;
            }

            CarpoolingRequest bestRider = bestMatch.getRider();
            CarpoolingUtil.setRequestStatus(bestRider.getLeg(), "matched");
            for (PlanElement planElement : bestRider.getPerson().getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    if (((Activity) planElement).getEndTime().isDefined()) {
                        if (((Activity) planElement).getEndTime().seconds() == bestRider
                                .getDepartureTime()) {
                            CarpoolingUtil.setLinkageActivityToRiderRequest((Activity) planElement,
                                    bestRider.getId().toString());
                            break;
                        }
                    }
                }
            }
            LOGGER.info(driverRequest.getPerson().getId() + "'s best rider match is "
                    + bestRider.getPerson().getId() + ". Pickup point is "
                    + bestRider.getFromLink().getId());
            matches.add(bestMatch);
            iterator.remove();
            requestsRegister.removeRequest(bestRider);
        }

        unmatchedDriverRequests = driverRequests;

        Set<CarpoolingRequest> matchedRiders = matches.stream().map(CarpoolingMatch::getRider)
                .collect(Collectors.toSet());
        unmatchedRiderRequests = new ArrayList<>();
        for (CarpoolingRequest request : riderRequests) {
            if (!matchedRiders.contains(request)) {
                unmatchedRiderRequests.add(request);
            }
        }

        LOGGER.info(matches.size() + " matches happened. Matching process finished!");
    }

    public List<CarpoolingMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    public List<CarpoolingRequest> getUnmatchedDriverRequests() {
        return Collections.unmodifiableList(unmatchedDriverRequests);
    }

    public List<CarpoolingRequest> getUnmatchedRiderRequests() {
        return Collections.unmodifiableList(unmatchedRiderRequests);
    }

}
