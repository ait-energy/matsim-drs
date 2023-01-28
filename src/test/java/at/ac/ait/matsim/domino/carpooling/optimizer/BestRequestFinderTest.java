package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class BestRequestFinderTest {
    Network network = NetworkUtils.readNetwork("data/floridsdorf/network_carpooling.xml");
    LeastCostPathCalculator router = new SpeedyDijkstra(new SpeedyGraph(network),new FreeSpeedTravelTime(),new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
    BestRequestFinder bestRequestFinder = new BestRequestFinder(router,new CarpoolingConfigGroup("cfgGroup"));
    CarpoolingRequest driverRequest = new CarpoolingRequest(Id.create(1, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1540)),network.getLinks().get(Id.createLinkId(186)));
    CarpoolingRequest request2 = new CarpoolingRequest(Id.create(2, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1541)),network.getLinks().get(Id.createLinkId(186)));
    LeastCostPathCalculator.Path toRequest2 = router.calcLeastCostPath(driverRequest.getFromLink().getFromNode(),
            request2.getFromLink().getFromNode(), 0, null, null);
    CarpoolingRequest request3 = new CarpoolingRequest(Id.create(3, Request.class),null,null,11*60*60,null,network.getLinks().get(Id.createLinkId(1037)),network.getLinks().get(Id.createLinkId(186)));
    LeastCostPathCalculator.Path toRequest3 = router.calcLeastCostPath(driverRequest.getFromLink().getFromNode(),
            request3.getFromLink().getFromNode(), 0, null, null);
    CarpoolingRequest request4 = new CarpoolingRequest(Id.create(4, Request.class),null,null,7*60*60,null,network.getLinks().get(Id.createLinkId(186)),network.getLinks().get(Id.createLinkId(1037)));
    LeastCostPathCalculator.Path toRequest4 = router.calcLeastCostPath(driverRequest.getFromLink().getFromNode(),
            request4.getFromLink().getFromNode(), 0, null, null);
    CarpoolingRequest request5 = new CarpoolingRequest(Id.create(5, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(688)),network.getLinks().get(Id.createLinkId(1540)));
    LeastCostPathCalculator.Path toRequest5 = router.calcLeastCostPath(driverRequest.getFromLink().getFromNode(),
            request5.getFromLink().getFromNode(), 0, null, null);

    HashMap<CarpoolingRequest, LeastCostPathCalculator.Path> filteredPassengersRequests = new HashMap<>();

    @Test
    void noFilteredRequests(){
        assertNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
    }

    @Test
    void noRequestsLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,toRequest4);
        filteredPassengersRequests.put(request5,toRequest5);
        assertNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
    }

    @Test
    void oneRequestLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,toRequest4);
        filteredPassengersRequests.put(request5,toRequest5);
        filteredPassengersRequests.put(request3,toRequest3);
        assertNotNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
        assertEquals("3",bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests).getId().toString());
    }

    @Test
    void moreThanOneRequestLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,toRequest4);
        filteredPassengersRequests.put(request5,toRequest5);
        filteredPassengersRequests.put(request3,toRequest3);
        filteredPassengersRequests.put(request2,toRequest2);
        assertNotNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
        assertEquals("3",bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests).getId().toString());
    }
}