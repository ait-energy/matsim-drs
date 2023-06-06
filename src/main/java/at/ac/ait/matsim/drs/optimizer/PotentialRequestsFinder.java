package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Node;

import at.ac.ait.matsim.drs.request.CarpoolingRequest;
import at.ac.ait.matsim.drs.run.CarpoolingConfigGroup;

public class PotentialRequestsFinder {
    private final CarpoolingConfigGroup cfgGroup;
    private final RequestsRegister requestsRegister;

    public PotentialRequestsFinder(CarpoolingConfigGroup cfgGroup, RequestsRegister requestsRegister) {
        this.cfgGroup = cfgGroup;
        this.requestsRegister = requestsRegister;
    }

    public List<CarpoolingRequest> findRegistryIntersections(Node origin, Node destination, double departureTime) {
        return getIntersection(cfgGroup, requestsRegister.getOriginZonalRegistry().findNearestRequests(origin),
                requestsRegister.getDestinationZonalRegistry().findNearestRequests(destination),
                requestsRegister.getTimeSegmentRegistry().findNearestRequests(departureTime));
    }

    static List<CarpoolingRequest> getIntersection(CarpoolingConfigGroup cfgGroup,
            Stream<CarpoolingRequest> originNearRequests, Stream<CarpoolingRequest> destinationNearRequests,
            Stream<CarpoolingRequest> temporalNearRequests) {
        Stream<CarpoolingRequest> zonalRegistryIntersection = originNearRequests
                .filter(destinationNearRequests.collect(Collectors.toList())::contains);
        return zonalRegistryIntersection.filter(temporalNearRequests.collect(Collectors.toList())::contains)
                .limit(cfgGroup.getMaxPossibleCandidates()).collect(Collectors.toCollection(ArrayList::new));
    }
}
