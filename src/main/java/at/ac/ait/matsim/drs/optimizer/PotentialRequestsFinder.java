package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Node;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class PotentialRequestsFinder {
    private final DrsConfigGroup cfgGroup;
    private final RequestsRegister requestsRegister;

    public PotentialRequestsFinder(DrsConfigGroup cfgGroup, RequestsRegister requestsRegister) {
        this.cfgGroup = cfgGroup;
        this.requestsRegister = requestsRegister;
    }

    public List<DrsRequest> findRegistryIntersections(Node origin, Node destination, double departureTime) {
        return getIntersection(cfgGroup, requestsRegister.getOriginZoneRegistry().findNearestRequests(origin),
                requestsRegister.getDestinationZoneRegistry().findNearestRequests(destination),
                requestsRegister.getTimeSegmentRegistry().findNearestRequests(departureTime));
    }

    static List<DrsRequest> getIntersection(DrsConfigGroup cfgGroup,
            Stream<DrsRequest> originNearRequests, Stream<DrsRequest> destinationNearRequests,
            Stream<DrsRequest> temporalNearRequests) {
        Stream<DrsRequest> zoneRegistryIntersection = originNearRequests
                .filter(destinationNearRequests.collect(Collectors.toList())::contains);
        return zoneRegistryIntersection.filter(temporalNearRequests.collect(Collectors.toList())::contains)
                .limit(cfgGroup.getMaxPossibleCandidates()).collect(Collectors.toCollection(ArrayList::new));
    }
}
