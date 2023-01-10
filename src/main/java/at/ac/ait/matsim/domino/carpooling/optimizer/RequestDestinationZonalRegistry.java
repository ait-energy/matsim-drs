package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

public class RequestDestinationZonalRegistry {
    private final Network network;
    private final ZonalSystem zonalSystem;
    // private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;
    private final Map<Id<Zone>, ArrayList<CarpoolingRequest>> requestsInZones;

    public RequestDestinationZonalRegistry(Network network, ZonalSystem zonalSystem, Map<Id<Zone>, ArrayList<CarpoolingRequest>>  requestsInZones) {
        this.network = network;
        this.zonalSystem = zonalSystem;
        //zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());
        this.requestsInZones = requestsInZones;
    }

    public void addRequest(CarpoolingRequest request) {
        Id<Zone> zoneId = getZoneId(request);
        requestsInZones.get(zoneId).add(request);
    }

    public void removeRequest(CarpoolingRequest request) {
        Id<Zone> zoneId = getZoneId(request);
        requestsInZones.get(zoneId).remove(request);
    }

/*    public Stream<CarpoolingRequest> findNearestRequests(Node node, int minCount) {
        return zonesSortedByDistance.get(zonalSystem.getZone(node).getId())
                .stream()
                .flatMap(z -> requestsInZones.get(z.getId()).values().stream())
                .limit(minCount);
    }*/

    public Stream<CarpoolingRequest> findNearestRequests(Node node, int minCount) {
        return requestsInZones.get(zonalSystem.getZone(node).getId()).stream().limit(minCount);
    }


    private Id<Zone> getZoneId(CarpoolingRequest request) {
        return zonalSystem.getZone(NetworkUtils.getNearestNode(network,request.getDestination())).getId();
    }

}