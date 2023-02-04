package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("all")

class RequestZonalRegistryTest {

    Network network = NetworkUtils.readNetwork("data/floridsdorf/network_carpooling.xml");
    ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(),new CarpoolingConfigGroup("cfgGroup").cellSize);
    RequestZonalRegistry zonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,true);
    CarpoolingRequest request1 = new CarpoolingRequest(Id.create(1, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1540)),null);
    CarpoolingRequest request2 = new CarpoolingRequest(Id.create(2, Request.class),null,null,8*60*60,null, network.getLinks().get(Id.createLinkId(1037)),null);
    CarpoolingRequest request3 = new CarpoolingRequest(Id.create(3, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1674)),null);

    @Test
    void testGetZoneIdForNeighboringRequests(){
        assertSame(RequestZonalRegistry.getZoneId(request1,true,zonalSystem),RequestZonalRegistry.getZoneId(request2,true,zonalSystem));
    }

    @Test
    void testGetZoneIdForNotNeighboringRequests(){
        assertNotSame(RequestZonalRegistry.getZoneId(request1,true,zonalSystem),RequestZonalRegistry.getZoneId(request3,true,zonalSystem));
    }

    @Test
    void testAddRequestsToZones(){
        zonalRegistry.addRequest(request1);
        assertEquals(1, zonalRegistry.getRequestsInZones().get(RequestZonalRegistry.getZoneId(request1,true,zonalSystem)).size());
        zonalRegistry.addRequest(request2);
        assertEquals(2, zonalRegistry.getRequestsInZones().get(RequestZonalRegistry.getZoneId(request1,true,zonalSystem)).size());
        zonalRegistry.addRequest(request3);
        assertEquals(2, zonalRegistry.getRequestsInZones().get(RequestZonalRegistry.getZoneId(request1,true,zonalSystem)).size());
    }

    @Test
    void testAddSameRequestTwice(){
        zonalRegistry.addRequest(request1);
        assertThrows(RuntimeException.class,() ->{
            zonalRegistry.addRequest(request1);
        });
    }

    @Test
    void testRemoveRequestInRegistry(){
        zonalRegistry.addRequest(request1);
        zonalRegistry.addRequest(request2);
        Id<Zone> zoneId = RequestZonalRegistry.getZoneId(request1,true,zonalSystem);
        assertEquals(2, zonalRegistry.getRequestsInZones().get(zoneId).size());
        zonalRegistry.removeRequest(request1);
        assertEquals(1, zonalRegistry.getRequestsInZones().get(zoneId).size());
        zonalRegistry.removeRequest(request2);
        assertEquals(0, zonalRegistry.getRequestsInZones().get(zoneId).size());
    }

    @Test
    void testRemoveRequestNotInRegistry(){
        assertThrows(RuntimeException.class,() ->{
            zonalRegistry.removeRequest(request1);
        });
    }

    @Test
    void testFindNearestRequests(){
        zonalRegistry.addRequest(request1);
        zonalRegistry.addRequest(request2);
        zonalRegistry.addRequest(request3);
        assertEquals(0,zonalRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(857)).getFromNode()).count());
        assertEquals(1,zonalRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(1674)).getFromNode()).count());
        assertEquals(2,zonalRegistry.findNearestRequests(network.getLinks().get(Id.createLinkId(1541)).getFromNode()).count());
    }

}