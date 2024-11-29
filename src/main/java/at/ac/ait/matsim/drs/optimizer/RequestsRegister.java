package at.ac.ait.matsim.drs.optimizer;

import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class RequestsRegister {
    private final RequestZoneRegistry originZonRegistry;
    private final RequestZoneRegistry destinationZoneRegistry;
    private final RequestTimeSegmentRegistry timeSegmentRegistry;

    public RequestsRegister(DrsConfigGroup drsConfig, H3ZoneSystem h3Zones) {
        this.originZonRegistry = RequestZoneRegistry.forOrigins(h3Zones);
        this.destinationZoneRegistry = RequestZoneRegistry.forDestinations(h3Zones);
        this.timeSegmentRegistry = new RequestTimeSegmentRegistry(drsConfig);
    }

    public void addRequest(DrsRequest request) {
        originZonRegistry.addRequest(request);
        destinationZoneRegistry.addRequest(request);
        timeSegmentRegistry.addRequest(request);
    }

    public void removeRequest(DrsRequest request) {
        originZonRegistry.removeRequest(request);
        destinationZoneRegistry.removeRequest(request);
        timeSegmentRegistry.removeRequest(request);
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
