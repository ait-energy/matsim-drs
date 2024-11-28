package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Node;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class PotentialRequestsFinder {
    private final DrsConfigGroup drsConfig;
    private final RequestsRegister requestsRegister;

    public PotentialRequestsFinder(DrsConfigGroup drsConfig, RequestsRegister requestsRegister) {
        this.drsConfig = drsConfig;
        this.requestsRegister = requestsRegister;
    }

    public List<DrsRequest> findRegistryIntersections(Node origin, Node destination, double departureTime) {
        return getIntersection(drsConfig,
                requestsRegister.getOriginZoneRegistry().findRequestsWithinDistance(origin,
                        drsConfig.getMaxMatchingDistanceMeters()),
                requestsRegister.getDestinationZoneRegistry().findRequestsWithinDistance(destination,
                        drsConfig.getMaxMatchingDistanceMeters()),
                requestsRegister.getTimeSegmentRegistry().findNearestRequests(departureTime));
    }

    static List<DrsRequest> getIntersection(DrsConfigGroup drsConfig,
            Stream<DrsRequest> originNearRequests, Stream<DrsRequest> destinationNearRequests,
            Stream<DrsRequest> temporalNearRequests) {
        Stream<DrsRequest> zoneRegistryIntersection = originNearRequests
                .filter(destinationNearRequests.collect(Collectors.toList())::contains);
        return zoneRegistryIntersection.filter(temporalNearRequests.collect(Collectors.toList())::contains)
                .limit(drsConfig.getMaxPossibleCandidates())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
