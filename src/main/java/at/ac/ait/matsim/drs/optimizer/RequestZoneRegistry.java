package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.systems.grid.h3.H3Utils;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.network.NetworkUtils;

import com.uber.h3core.H3Core;

/**
 * Geographically matches each request to a H3 cell (zone)
 */
public class RequestZoneRegistry {
    private final H3ZoneSystem zoneSystem;
    private final boolean isOriginZoneRegistry;
    private final Map<Id<Zone>, Map<Id<Request>, DrsRequest>> requestsInZones;
    private final H3Core h3;

    private RequestZoneRegistry(H3ZoneSystem zoneSystem, boolean isOriginZoneRegistry) {
        this.zoneSystem = zoneSystem;
        this.isOriginZoneRegistry = isOriginZoneRegistry;
        requestsInZones = new HashMap<>(zoneSystem.getZones().size());
        for (Id<Zone> id : zoneSystem.getZones().keySet()) {
            requestsInZones.put(id, new LinkedHashMap<>());
        }
        h3 = H3Utils.getInstance();
    }

    public static RequestZoneRegistry forOrigins(H3ZoneSystem zoneSystem) {
        return new RequestZoneRegistry(zoneSystem, true);
    }

    public static RequestZoneRegistry forDestinations(H3ZoneSystem zoneSystem) {
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

    /**
     * Uses neighbouring H3 cells to guarantee finding all requests within the given
     * max distance (this guarantee only holds if the given distance does not exceed
     * the value used for calculating the H3 resolution)
     */
    public Stream<DrsRequest> findRequestsWithinDistance(Node node, int maxEuclideanDistance) {
        Id<Zone> centerZoneId = getZoneId(node);
        Long cellId = Long.parseLong(centerZoneId.toString());

        List<Long> disk = h3.gridDisk(cellId, 1);
        List<Id<Zone>> diskZones = disk.stream().map(id -> Id.create(id, Zone.class)).collect(Collectors.toList());

        List<DrsRequest> potentialRequests = new ArrayList<>();
        for (var zoneId : diskZones) {
            potentialRequests.addAll(requestsInZones.getOrDefault(zoneId, Map.of()).values());
        }

        return potentialRequests.stream()
                .filter(r -> NetworkUtils.getEuclideanDistance(node.getCoord(),
                        getRequestNode(r).getCoord()) < maxEuclideanDistance);
    }

    public Map<Id<Zone>, Map<Id<Request>, DrsRequest>> getRequestsInZones() {
        return requestsInZones;
    }

    public long requestCount() {
        return requestsInZones.values().stream().flatMap(m -> m.values().stream()).count();
    }

    Node getRequestNode(DrsRequest request) {
        return isOriginZoneRegistry ? request.getFromNode() : request.getToNode();
    }

    Id<Zone> getZoneId(DrsRequest request) {
        return getZoneId(getRequestNode(request));
    }

    Id<Zone> getZoneId(Node node) {
        Optional<Zone> zone = zoneSystem.getZoneForNodeId(node.getId());
        if (!zone.isPresent()) {
            throw new IllegalArgumentException("no zone found for node " + node.getId());
        }
        return zone.get().getId();
    }
}
