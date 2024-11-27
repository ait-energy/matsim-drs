package at.ac.ait.matsim.drs.optimizer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * Heavily inspired by
 * org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry
 */
public class RequestZoneRegistry {
    private final ZoneSystem zoneSystem;
    private final boolean isOriginZoneRegistry;
    private final Map<Id<Zone>, Map<Id<Request>, DrsRequest>> requestsInZones;

    private RequestZoneRegistry(ZoneSystem zoneSystem, boolean isOriginZoneRegistry) {
        this.zoneSystem = zoneSystem;
        this.isOriginZoneRegistry = isOriginZoneRegistry;
        requestsInZones = new HashMap<>(zoneSystem.getZones().size());
        for (Id<Zone> id : zoneSystem.getZones().keySet()) {
            requestsInZones.put(id, new LinkedHashMap<>());
        }
    }

    public static RequestZoneRegistry forOrigins(ZoneSystem zoneSystem) {
        return new RequestZoneRegistry(zoneSystem, true);
    }

    public static RequestZoneRegistry forDestinations(ZoneSystem zoneSystem) {
        return new RequestZoneRegistry(zoneSystem, false);
    }

    public void addRequest(DrsRequest request) {
        Id<Zone> zoneId = getZoneId(request);
        if (requestsInZones.get(zoneId).put(request.getId(), request) != null) {
            throw new IllegalStateException(request + " is already in the registry");
        }
    }

    public void removeRequest(DrsRequest request) {
        Id<Zone> zoneId = getZoneId(request);
        if (requestsInZones.get(zoneId).remove(request.getId()) == null) {
            throw new IllegalStateException(request + " is not in the registry");
        }
    }

    public Stream<DrsRequest> findNearestRequests(Node node) {
        return requestsInZones.get(getZoneId(node)).values().stream();
    }

    public Map<Id<Zone>, Map<Id<Request>, DrsRequest>> getRequestsInZones() {
        return requestsInZones;
    }

    public Id<Zone> getZoneId(DrsRequest request) {
        Node node = isOriginZoneRegistry ? request.getFromLink().getFromNode() : request.getToLink().getToNode();
        return getZoneId(node);
    }

    public Id<Zone> getZoneId(Node node) {
        Optional<Zone> zone = zoneSystem.getZoneForNodeId(node.getId());
        if (!zone.isPresent()) {
            throw new IllegalArgumentException("no zone found for node " + node.getId());
        }
        return zone.get().getId();
    }
}
