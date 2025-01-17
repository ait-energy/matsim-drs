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
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

/**
 * Find best matches.
 *
 * Guarantees that there are no partial matches for a single person, i.e. either
 * all or none of a person's rider legs are matched. This makes it easy for the
 * conflict logic to discard invalid plans.
 */
public class MatchMaker {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DrsConfigGroup drsConfig;
    private final List<DrsDriverRequest> driverRequests;
    private final List<DrsRiderRequest> originalRiderRequests;
    private final List<DrsRiderRequest> riderRequests;
    private final RequestsRegister requestsRegister;
    private final BestMatchFinder bestMatchFinder;
    private final PotentialMatchFinder potentialMatchFinder;

    private List<DrsMatch> matches;
    private List<DrsDriverRequest> unmatchedDriverRequests;
    private List<DrsRiderRequest> unmatchedRiderRequests;

    public MatchMaker(DrsConfigGroup drsConfig,
            List<DrsDriverRequest> driverRequests,
            List<DrsRiderRequest> riderRequests,
            RequestsRegister requestsRegister,
            PotentialMatchFinder potentialMatchFinder,
            BestMatchFinder bestMatchFinder) {
        this.drsConfig = drsConfig;
        // mutable copies of the requests
        this.driverRequests = Lists.newArrayList(driverRequests);
        Collections.shuffle(this.driverRequests);
        this.riderRequests = Lists.newArrayList(riderRequests);
        Collections.shuffle(this.riderRequests);
        this.originalRiderRequests = List.copyOf(riderRequests);

        this.requestsRegister = requestsRegister;
        this.potentialMatchFinder = potentialMatchFinder;
        this.bestMatchFinder = bestMatchFinder;
    }

    public MatchingResult match() {
        if (matches != null) {
            throw new RuntimeException("Can only match once");
        }

        for (DrsRiderRequest ridersRequest : riderRequests) {
            requestsRegister.addRequest(ridersRequest);
        }

        matches = new ArrayList<>();
        for (Iterator<DrsDriverRequest> iterator = driverRequests.iterator(); iterator.hasNext();) {
            DrsDriverRequest driverRequest = iterator.next();
            List<DrsRiderRequest> potentialRiders = findPotentialRiders(
                    driverRequest.getFromNode(), driverRequest.getToNode(),
                    driverRequest.getDepartureTime());

            List<DrsMatch> filteredMatches = potentialMatchFinder.filterRequests(driverRequest, potentialRiders);
            DrsMatch bestMatch = bestMatchFinder.findBestMatch(filteredMatches);
            if (bestMatch == null) {
                continue;
            }

            DrsRiderRequest bestRider = bestMatch.getRider();
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

        Set<DrsRiderRequest> matchedRiders = matches.stream().map(DrsMatch::getRider)
                .collect(Collectors.toSet());
        unmatchedRiderRequests = new ArrayList<>();
        for (DrsRiderRequest request : riderRequests) {
            if (!matchedRiders.contains(request)) {
                unmatchedRiderRequests.add(request);
            }
        }

        LOGGER.debug("Initially {} matches found.", matches.size());
        enforceCompleteMatchForEachPerson();
        LOGGER.debug("{} matches remain after enforcing none or all rider legs matched.", matches.size());
        return new MatchingResult(Collections.unmodifiableList(matches),
                Collections.unmodifiableList(unmatchedDriverRequests),
                Collections.unmodifiableList(unmatchedRiderRequests));
    }

    public List<DrsRiderRequest> findPotentialRiders(Node origin, Node destination, double departureTime) {
        return findPotentialRiders(drsConfig,
                requestsRegister.getOriginZoneRegistry().findRequestsWithinDistance(origin,
                        drsConfig.maxMatchingDistanceMeters),
                requestsRegister.getDestinationZoneRegistry().findRequestsWithinDistance(destination,
                        drsConfig.maxMatchingDistanceMeters),
                requestsRegister.getTimeSegmentRegistry().findNearestRequests(departureTime));
    }

    static List<DrsRiderRequest> findPotentialRiders(DrsConfigGroup drsConfig,
            Stream<? extends DrsRequest> originNearRequests,
            Stream<? extends DrsRequest> destinationNearRequests,
            Stream<? extends DrsRequest> temporalNearRequests) {
        Stream<DrsRiderRequest> zoneRegistryIntersection = originNearRequests
                .filter(destinationNearRequests.collect(Collectors.toList())::contains)
                .filter(DrsRiderRequest.class::isInstance)
                .map(DrsRiderRequest.class::cast);
        return zoneRegistryIntersection.filter(temporalNearRequests.collect(Collectors.toList())::contains)
                .limit(drsConfig.maxPossibleCandidates)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void enforceCompleteMatchForEachPerson() {
        Multimap<Id<Person>, DrsRiderRequest> person2riderRequest = originalRiderRequests.stream()
                .collect(
                        ImmutableListMultimap.toImmutableListMultimap(r -> r.getPerson().getId(), Function.identity()));

        List<DrsRequest> matchedRiderRequests = matches.stream().map(m -> m.getRider()).collect(Collectors.toList());

        Set<DrsRequest> undesiredMatches = new HashSet<>();
        for (Id<Person> personId : person2riderRequest.keySet()) {
            Collection<DrsRiderRequest> requests = person2riderRequest.get(personId);
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

}
