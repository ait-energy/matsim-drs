package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;

public class RequestsRegister {
    private final RequestZonalRegistry originZonalRegistry;
    private final RequestZonalRegistry destinationZonalRegistry;
    private final RequestTimeSegmentRegistry timeSegmentRegistry;

    public RequestsRegister(RequestZonalRegistry originZonalRegistry, RequestZonalRegistry destinationZonalRegistry, RequestTimeSegmentRegistry timeSegmentRegistry) {
        this.originZonalRegistry = originZonalRegistry;
        this.destinationZonalRegistry = destinationZonalRegistry;
        this.timeSegmentRegistry = timeSegmentRegistry;
    }

    public void addRequest(CarpoolingRequest passengersRequest) {
        originZonalRegistry.addRequest(passengersRequest);
        destinationZonalRegistry.addRequest(passengersRequest);
        timeSegmentRegistry.addRequest(passengersRequest);
    }

    public void removeRequest(CarpoolingRequest passengersRequest) {
        originZonalRegistry.removeRequest(passengersRequest);
        destinationZonalRegistry.removeRequest(passengersRequest);
        timeSegmentRegistry.removeRequest(passengersRequest);
    }

    public RequestZonalRegistry getOriginZonalRegistry() {
        return originZonalRegistry;
    }

    public RequestZonalRegistry getDestinationZonalRegistry() {
        return destinationZonalRegistry;
    }

    public RequestTimeSegmentRegistry getTimeSegmentRegistry() {
        return timeSegmentRegistry;
    }

}
