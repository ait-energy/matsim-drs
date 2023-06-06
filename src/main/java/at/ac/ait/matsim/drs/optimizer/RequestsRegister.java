package at.ac.ait.matsim.drs.optimizer;

import at.ac.ait.matsim.drs.request.DrsRequest;

public class RequestsRegister {
    private final RequestZonalRegistry originZonalRegistry;
    private final RequestZonalRegistry destinationZonalRegistry;
    private final RequestTimeSegmentRegistry timeSegmentRegistry;

    public RequestsRegister(RequestZonalRegistry originZonalRegistry, RequestZonalRegistry destinationZonalRegistry,
            RequestTimeSegmentRegistry timeSegmentRegistry) {
        this.originZonalRegistry = originZonalRegistry;
        this.destinationZonalRegistry = destinationZonalRegistry;
        this.timeSegmentRegistry = timeSegmentRegistry;
    }

    public void addRequest(DrsRequest riderRequest) {
        originZonalRegistry.addRequest(riderRequest);
        destinationZonalRegistry.addRequest(riderRequest);
        timeSegmentRegistry.addRequest(riderRequest);
    }

    public void removeRequest(DrsRequest riderRequest) {
        originZonalRegistry.removeRequest(riderRequest);
        destinationZonalRegistry.removeRequest(riderRequest);
        timeSegmentRegistry.removeRequest(riderRequest);
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
