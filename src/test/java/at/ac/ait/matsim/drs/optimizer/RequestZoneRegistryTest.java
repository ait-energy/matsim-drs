package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.core.network.NetworkUtils;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
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
        DrsConfigGroup cfg = new DrsConfigGroup();
        cfg.setCellSize(800);
        zoneSystem = new SquareGridZoneSystem(network, cfg.getCellSize());
        request1 = DrsTestUtil.mockRiderRequest(1, 8 * 60 * 60,
                network.getLinks().get(Id.createLinkId(1540)), null);
        request2 = DrsTestUtil.mockRiderRequest(2, 8 * 60 * 60,
                network.getLinks().get(Id.createLinkId(1037)), null);
        request3 = DrsTestUtil.mockRiderRequest(3, 8 * 60 * 60,
                network.getLinks().get(Id.createLinkId(1674)), null);
    }

    @BeforeEach
    public void beforeEach() {
        zoneRegistry = RequestZoneRegistry.createRequestZoneRegistry(zoneSystem, true);
    }

    @Test
    void testGetZoneIdForNeighboringRequests() {
        assertSame(RequestZoneRegistry.getZoneId(request1, true, zoneSystem),
                RequestZoneRegistry.getZoneId(request2, true, zoneSystem));
    }

    @Test
    void testGetZoneIdForNotNeighboringRequests() {
        assertNotSame(RequestZoneRegistry.getZoneId(request1, true, zoneSystem),
                RequestZoneRegistry.getZoneId(request3, true, zoneSystem));
    }

    @Test
    void testAddRequestsToZones() {
        zoneRegistry.addRequest(request1);
        assertEquals(1, zoneRegistry.getRequestsInZones()
                .get(RequestZoneRegistry.getZoneId(request1, true, zoneSystem)).size());
        zoneRegistry.addRequest(request2);
        assertEquals(2, zoneRegistry.getRequestsInZones()
                .get(RequestZoneRegistry.getZoneId(request1, true, zoneSystem)).size());
        zoneRegistry.addRequest(request3);
        assertEquals(2, zoneRegistry.getRequestsInZones()
                .get(RequestZoneRegistry.getZoneId(request1, true, zoneSystem)).size());
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
        Id<Zone> zoneId = RequestZoneRegistry.getZoneId(request1, true, zoneSystem);
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
                zoneRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(857)).getFromNode()).count());
        assertEquals(1,
                zoneRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(1674)).getFromNode()).count());
        assertEquals(2,
                zoneRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(1541)).getFromNode()).count());
    }

}