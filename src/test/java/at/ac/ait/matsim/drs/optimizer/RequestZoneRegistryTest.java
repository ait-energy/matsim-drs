package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.core.network.NetworkUtils;

import com.google.common.base.Predicates;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

class RequestZoneRegistryTest {

    private static Network network;
    private static H3ZoneSystem zoneSystem;
    private static DrsRequest req1084, req1085, req1086, req1087, req1159, req1160, req1161;
    private static List<DrsRequest> allReqs;

    private RequestZoneRegistry zoneRegistry;

    @BeforeAll
    public static void setup() {
        network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");
        DrsUtil.addNewAllowedModeToCarLinks(network, Drs.DRIVER_MODE);
        // consecutive links on Kugelfanggasse (~900m)
        req1084 = req(1084);
        req1085 = req(1085);
        req1086 = req(1086);
        req1087 = req(1087);
        req1159 = req(1159);
        req1160 = req(1160);
        req1161 = req(1161);
        allReqs = List.of(req1084, req1085, req1086, req1087, req1159, req1160, req1161);
    }

    @BeforeEach
    public void beforeEach() {
        zoneSystem = new H3ZoneSystem("EPSG:31256", 9, network, Predicates.alwaysTrue());
        zoneRegistry = RequestZoneRegistry.forOrigins(zoneSystem);
    }

    @Test
    public void testGetZoneId() {
        assertEquals("891e15b600fffff", hex(zoneRegistry.getZoneId(req1084)));

        assertEquals("891e15b6077ffff", hex(zoneRegistry.getZoneId(req1085)));
        assertEquals("891e15b6077ffff", hex(zoneRegistry.getZoneId(req1086)));
        assertEquals("891e15b6077ffff", hex(zoneRegistry.getZoneId(req1087)));

        assertEquals("891e15b602bffff", hex(zoneRegistry.getZoneId(req1159)));
        assertEquals("891e15b602bffff", hex(zoneRegistry.getZoneId(req1160)));

        assertEquals("891e15b6393ffff", hex(zoneRegistry.getZoneId(req1161)));
    }

    @Test
    public void testAddRequestsToZones() {
        assertEquals(0, zoneRegistry.requestCount());

        zoneRegistry.addRequest(req1084);
        assertEquals(1, zoneRegistry.requestCount());
        assertEquals(1, zoneRegistry.getRequestsInZones().get(zoneRegistry.getZoneId(req1084)).size());
        assertEquals(0, zoneRegistry.getRequestsInZones().get(zoneRegistry.getZoneId(req1085)).size());

        zoneRegistry.addRequest(req1085);
        zoneRegistry.addRequest(req1086);
        zoneRegistry.addRequest(req1087);
        assertEquals(4, zoneRegistry.requestCount());
        assertEquals(1, zoneRegistry.getRequestsInZones().get(zoneRegistry.getZoneId(req1084)).size());
        assertEquals(3, zoneRegistry.getRequestsInZones().get(zoneRegistry.getZoneId(req1085)).size());
    }

    @Test
    public void testAddSameRequestTwice() {
        zoneRegistry.addRequest(req1084);
        assertThrows(RuntimeException.class, () -> {
            zoneRegistry.addRequest(req1084);
        });
    }

    @Test
    public void testRemoveRequestInRegistry() {
        zoneRegistry.addRequest(req1085);
        zoneRegistry.addRequest(req1086);
        Id<Zone> zoneId = zoneRegistry.getZoneId(req1085);
        assertEquals(2, zoneRegistry.getRequestsInZones().get(zoneId).size());
        zoneRegistry.removeRequest(req1085);
        assertEquals(1, zoneRegistry.getRequestsInZones().get(zoneId).size());
        zoneRegistry.removeRequest(req1086);
        assertEquals(0, zoneRegistry.getRequestsInZones().get(zoneId).size());
    }

    @Test
    public void testRemoveRequestNotInRegistry() {
        assertThrows(RuntimeException.class, () -> {
            zoneRegistry.removeRequest(req1084);
        });
    }

    @Test
    public void findRequestsWithinDistance_1_withinCell() {
        allReqs.forEach(zoneRegistry::addRequest);

        Set<DrsRequest> reqs = find(req1084, 1);
        assertEquals(Set.of(req1084), reqs);
    }

    @Test
    public void findRequestsWithinDistance_125_withinCell() {
        allReqs.forEach(zoneRegistry::addRequest);

        Set<DrsRequest> reqs = find(req1085, 125);
        assertEquals(Set.of(req1085, req1086), reqs);
    }

    @Test
    public void findRequestsWithinDistance_170_acrossCells() {
        allReqs.forEach(zoneRegistry::addRequest);

        Set<DrsRequest> reqs = find(req1085, 170);
        assertEquals(Set.of(req1084, req1085, req1086), reqs);
    }

    @Test
    public void findRequestsWithinDistance_200_acrossCells() {
        allReqs.forEach(zoneRegistry::addRequest);

        Set<DrsRequest> reqs = find(req1084, 200);
        assertEquals(Set.of(req1084, req1085), reqs);
    }

    private Set<DrsRequest> find(DrsRequest reference, int distance) {
        return zoneRegistry.findRequestsWithinDistance(reference.getFromLink().getFromNode(), distance)
                .collect(Collectors.toSet());
    }

    public static DrsRiderRequest req(int linkId) {
        Link link = network.getLinks().get(Id.createLinkId(linkId));
        return DrsTestUtil.mockRiderRequest(linkId, 0, link, null);
    }

    public static String hex(Id<Zone> id) {
        return Long.toHexString(Long.parseLong(id.toString()));
    }

}