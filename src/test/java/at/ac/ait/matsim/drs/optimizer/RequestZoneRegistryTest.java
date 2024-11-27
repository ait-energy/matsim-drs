package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.core.network.NetworkUtils;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

class RequestZoneRegistryTest {

    static Network network;
    static ZoneSystem zoneSystem;
    static DrsRequest request1, request2, request3;
    RequestZoneRegistry zoneRegistry;

    @BeforeAll
    public static void setup() {
        network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");
        DrsUtil.addNewAllowedModeToCarLinks(network, Drs.DRIVER_MODE);
    }

    @BeforeEach
    public void beforeEach() {
        zoneSystem = new SquareGridZoneSystem(network, 800);
        zoneRegistry = RequestZoneRegistry.forOrigins(zoneSystem);
        request1 = req(1, 1540);
        request2 = req(2, 1037);
        request3 = req(3, 1674);
    }

    // @Test
    // public void testAllRequestsWithinDistance() {
    // List<DrsRiderRequest> requests = List.of(req(1084), req(1085), req(1086),
    // req(1159), req(1160), req(1161));
    // }

    @Test
    void testGetZoneIdForNeighboringRequests() {
        assertEquals(zoneRegistry.getZoneId(request1), zoneRegistry.getZoneId(request2));
    }

    @Test
    void testGetZoneIdForNotNeighboringRequests() {
        assertNotEquals(zoneRegistry.getZoneId(request1), zoneRegistry.getZoneId(request3));
    }

    @Test
    void testAddRequestsToZones() {
        zoneRegistry.addRequest(request1);
        assertEquals(1, zoneRegistry.getRequestsInZones()
                .get(zoneRegistry.getZoneId(request1)).size());
        zoneRegistry.addRequest(request2);
        assertEquals(2, zoneRegistry.getRequestsInZones()
                .get(zoneRegistry.getZoneId(request1)).size());
        zoneRegistry.addRequest(request3);
        assertEquals(2, zoneRegistry.getRequestsInZones()
                .get(zoneRegistry.getZoneId(request1)).size());
    }

    @Test
    void testAddSameRequestTwice() {
        zoneRegistry.addRequest(request1);
        assertThrows(RuntimeException.class, () -> {
            zoneRegistry.addRequest(request1);
        });
    }

    @Test
    void testRemoveRequestInRegistry() {
        zoneRegistry.addRequest(request1);
        zoneRegistry.addRequest(request2);
        Id<Zone> zoneId = zoneRegistry.getZoneId(request1);
        assertEquals(2, zoneRegistry.getRequestsInZones().get(zoneId).size());
        zoneRegistry.removeRequest(request1);
        assertEquals(1, zoneRegistry.getRequestsInZones().get(zoneId).size());
        zoneRegistry.removeRequest(request2);
        assertEquals(0, zoneRegistry.getRequestsInZones().get(zoneId).size());
    }

    @Test
    void testRemoveRequestNotInRegistry() {
        assertThrows(RuntimeException.class, () -> {
            zoneRegistry.removeRequest(request1);
        });
    }

    @Test
    void testFindNearestRequests() {
        zoneRegistry.addRequest(request1);
        zoneRegistry.addRequest(request2);
        zoneRegistry.addRequest(request3);
        assertEquals(0,
                zoneRegistry.findNearestRequests(link(857).getFromNode()).count());
        assertEquals(1,
                zoneRegistry.findNearestRequests(link(1674).getFromNode()).count());
        assertEquals(2,
                zoneRegistry.findNearestRequests(link(1541).getFromNode()).count());
    }

    public DrsRiderRequest req(int linkId) {
        return DrsTestUtil.mockRiderRequest(linkId, 8 * 60 * 60, link(linkId), null);
    }

    public DrsRiderRequest req(int id, int linkId) {
        return DrsTestUtil.mockRiderRequest(id, 8 * 60 * 60, link(linkId), null);
    }

    public Link link(int linkId) {
        return network.getLinks().get(Id.createLinkId(linkId));
    }
}