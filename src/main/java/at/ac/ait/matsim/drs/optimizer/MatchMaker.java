package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Find best matches.
 *
 * Guarantees that there are no partial matches for a single person, i.e. either
 * all or none of a person's rider legs are matched. This makes it easy for the
 * conflict logic to discard invalid plans.
 */
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
            bestRider.setMatchedRequest(driverRequest.getId());
            driverRequest.setMatchedRequest(bestRider.getId());

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

        LOGGER.info("Initially {} matches found.", matches.size());
        enforceCompleteMatchForEachPerson();
        LOGGER.info("{} matches remain after enforcing none or all rider legs matched.", matches.size());
    }

    private void enforceCompleteMatchForEachPerson() {
        Multimap<Id<Person>, DrsRequest> person2riderRequest = requestsCollector.getRiderRequests()
                .stream().collect(
                        ImmutableListMultimap.toImmutableListMultimap(
                                r -> r.getPerson().getId(), Function.identity()));

        List<DrsRequest> matchedRiderRequests = matches.stream().map(m -> m.getRider()).collect(Collectors.toList());

        Set<DrsRequest> undesiredMatches = new HashSet<>();
        for (Id<Person> personId : person2riderRequest.keySet()) {
            Collection<DrsRequest> requests = person2riderRequest.get(personId);
            boolean allMatched = true;
            for (DrsRequest request : requests) {
                if (!matchedRiderRequests.contains(request)) {
                    allMatched = false;
                    break;
                }
            }
            if (!allMatched) {
                undesiredMatches.addAll(requests);
            }
        }

        Map<DrsRequest, DrsMatch> riderRequest2match = matches.stream()
                .collect(Collectors.toMap(m -> m.getRider(), Function.identity()));
        for (DrsRequest undesired : undesiredMatches) {
            if (riderRequest2match.containsKey(undesired)) {
                DrsMatch undesiredMatch = riderRequest2match.get(undesired);
                unmatchedDriverRequests.add(undesiredMatch.getDriver());
                unmatchedRiderRequests.add(undesiredMatch.getRider());
                matches.remove(undesiredMatch);
            }
        }
    }

    public MatchingResult getResult() {
        return new MatchingResult(Collections.unmodifiableList(matches),
                Collections.unmodifiableList(unmatchedDriverRequests),
                Collections.unmodifiableList(unmatchedRiderRequests));
    }

}
