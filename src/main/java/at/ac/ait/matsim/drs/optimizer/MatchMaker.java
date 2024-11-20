package at.ac.ait.matsim.drs.optimizer;

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
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.Lists;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();
    private final RequestsCollector requestsCollector;
    private final RequestsRegister requestsRegister;
    private final PotentialRequestsFinder potentialRequestsFinder;
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;

    private List<DrsMatch> matches;
    private List<DrsRequest> unmatchedDriverRequests;
    private List<DrsRequest> unmatchedRiderRequests;

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
        List<DrsRequest> driverRequests = Lists.newArrayList(requestsCollector.getDriverRequests());
        Collections.shuffle(driverRequests);
        List<DrsRequest> riderRequests = Lists.newArrayList(requestsCollector.getRiderRequests());
        Collections.shuffle(riderRequests);

        for (DrsRequest ridersRequest : riderRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        matches = new ArrayList<>();
        for (Iterator<DrsRequest> iterator = driverRequests.iterator(); iterator.hasNext();) {
            DrsRequest driverRequest = iterator.next();
            List<DrsRequest> potentialRequests = potentialRequestsFinder.findRegistryIntersections(
                    driverRequest.getFromLink().getFromNode(), driverRequest.getToLink().getFromNode(),
                    driverRequest.getDepartureTime());
            List<DrsMatch> filteredMatches = requestsFilter.filterRequests(driverRequest, potentialRequests);
            DrsMatch bestMatch = bestRequestFinder.findBestRequest(filteredMatches);

            if (bestMatch == null) {
                continue;
            }

            DrsRequest bestRider = bestMatch.getRider();
            DrsUtil.setRequestStatus(bestRider.getLeg(), Drs.REQUEST_STATUS_MATCHED);
            for (PlanElement planElement : bestRider.getPerson().getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    if (((Activity) planElement).getEndTime().isDefined()) {
                        if (((Activity) planElement).getEndTime().seconds() == bestRider
                                .getDepartureTime()) {
                            DrsUtil.setLinkageActivityToRiderRequest((Activity) planElement,
                                    bestRider.getId().toString());
                            break;
                        }
                    }
                }
            }
            LOGGER.debug("Match: {} should pick up {} on link {} @ {}",
                    driverRequest.getPerson().getId(),
                    bestRider.getPerson().getId(),
                    bestRider.getFromLink().getId(),
                    Time.writeTime(bestRider.getDepartureTime()));
            matches.add(bestMatch);
            iterator.remove();
            requestsRegister.removeRequest(bestRider);
        }

        unmatchedDriverRequests = driverRequests;

        Set<DrsRequest> matchedRiders = matches.stream().map(DrsMatch::getRider)
                .collect(Collectors.toSet());
        unmatchedRiderRequests = new ArrayList<>();
        for (DrsRequest request : riderRequests) {
            if (!matchedRiders.contains(request)) {
                unmatchedRiderRequests.add(request);
            }
        }

        LOGGER.info(matches.size() + " matches found.");
    }

    public List<DrsMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    public List<DrsRequest> getUnmatchedDriverRequests() {
        return Collections.unmodifiableList(unmatchedDriverRequests);
    }

    public List<DrsRequest> getUnmatchedRiderRequests() {
        return Collections.unmodifiableList(unmatchedRiderRequests);
    }

}
