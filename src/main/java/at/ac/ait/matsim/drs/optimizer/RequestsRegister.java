package at.ac.ait.matsim.drs.optimizer;

public class RequestsRegister {
    private final RequestZoneRegistry originZonRegistry;
    private final RequestZoneRegistry destinationZoneRegistry;
    private final RequestTimeSegmentRegistry timeSegmentRegistry;

    public RequestsRegister(RequestZoneRegistry originZoneRegistry, RequestZoneRegistry destinationZoneRegistry,
            RequestTimeSegmentRegistry timeSegmentRegistry) {
        this.originZonRegistry = originZoneRegistry;
        this.destinationZoneRegistry = destinationZoneRegistry;
        this.timeSegmentRegistry = timeSegmentRegistry;
    }

    public void addRequest(DrsRequest riderRequest) {
        originZonRegistry.addRequest(riderRequest);
        destinationZoneRegistry.addRequest(riderRequest);
        timeSegmentRegistry.addRequest(riderRequest);
    }

    public void removeRequest(DrsRequest riderRequest) {
        originZonRegistry.removeRequest(riderRequest);
        destinationZoneRegistry.removeRequest(riderRequest);
        timeSegmentRegistry.removeRequest(riderRequest);
    }

    public RequestZoneRegistry getOriginZoneRegistry() {
        return originZonRegistry;
    }

    public RequestZoneRegistry getDestinationZoneRegistry() {
        return destinationZoneRegistry;
    }

    public RequestTimeSegmentRegistry getTimeSegmentRegistry() {
        return timeSegmentRegistry;
    }

}
