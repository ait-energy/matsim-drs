package at.ac.ait.matsim.drs.optimizer;

import at.ac.ait.matsim.drs.request.DrsRequest;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;

import java.util.*;
import java.util.stream.Stream;

/**
 * Heavily inspired by
 * org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry
 */

public class RequestZonalRegistry {
    private final ZonalSystem zonalSystem;
    private final boolean isOriginZonalRegistry;
    private final Map<Id<Zone>, Map<Id<Request>, DrsRequest>> requestsInZones;

    private RequestZonalRegistry(ZonalSystem zonalSystem, boolean isOriginZonalRegistry) {
        this.zonalSystem = zonalSystem;
        this.isOriginZonalRegistry = isOriginZonalRegistry;
        requestsInZones = new HashMap<>(zonalSystem.getZones().size());
        for (Id<Zone> id : zonalSystem.getZones().keySet()) {
            requestsInZones.put(id, new LinkedHashMap<>());
        }
    }

    public static RequestZonalRegistry createRequestZonalRegistry(ZonalSystem zonalSystem,
            boolean isOriginZonalRegistry) {
        return new RequestZonalRegistry(zonalSystem, isOriginZonalRegistry);
    }

    public void addRequest(DrsRequest request) {
        Id<Zone> zoneId = getZoneId(request, isOriginZonalRegistry, zonalSystem);
        if (requestsInZones.get(zoneId).put(request.getId(), request) != null) {
            throw new IllegalStateException(request + " is already in the registry");
        }
    }

    public void removeRequest(DrsRequest request) {
        Id<Zone> zoneId = getZoneId(request, isOriginZonalRegistry, zonalSystem);
        if (requestsInZones.get(zoneId).remove(request.getId()) == null) {
            throw new IllegalStateException(request + " is not in the registry");
        }
    }

    public Stream<DrsRequest> findNearestRequests(Node node) {
        return requestsInZones.get(zonalSystem.getZone(node).getId()).values().stream();
    }

    public Map<Id<Zone>, Map<Id<Request>, DrsRequest>> getRequestsInZones() {
        return requestsInZones;
    }

    static Id<Zone> getZoneId(DrsRequest request, boolean isOriginZonalRegistry, ZonalSystem zonalSystem) {
        if (isOriginZonalRegistry) {
            return zonalSystem.getZone(request.getFromLink().getFromNode()).getId();
        } else {
            return zonalSystem.getZone(request.getToLink().getFromNode()).getId();
        }
    }
}
