package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.network.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NearestRequestsFinder {
    private final CarpoolingConfigGroup cfgGroup;
    private final RequestsRegister requestsRegister;

    public NearestRequestsFinder(CarpoolingConfigGroup cfgGroup, RequestsRegister requestsRegister) {
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
                .limit(cfgGroup.getNeighbourhoodSize()).collect(Collectors.toCollection(ArrayList::new));
    }
}
