package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.FacilitiesUtils;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BestRequestFinderTest {
    Network network = NetworkUtils.readNetwork("data/floridsdorf/network_carpooling.xml");
    TripRouter tripRouter;
    RoutingModule router = tripRouter.getRoutingModule("carpoolingDriver");
    BestRequestFinder bestRequestFinder = new BestRequestFinder(router,new CarpoolingConfigGroup("cfgGroup"));
    CarpoolingRequest driverRequest = new CarpoolingRequest(Id.create(1, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1540)),network.getLinks().get(Id.createLinkId(186)));
    CarpoolingRequest request2 = new CarpoolingRequest(Id.create(2, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(1541)),network.getLinks().get(Id.createLinkId(186)));
    RoutingRequest toRequest2 = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(request2.getFromLink()),FacilitiesUtils.wrapLink(request2.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
    List<? extends PlanElement> request2Route = router.calcRoute(toRequest2);
    CarpoolingRequest request3 = new CarpoolingRequest(Id.create(3, Request.class),null,null,11*60*60,null,network.getLinks().get(Id.createLinkId(1037)),network.getLinks().get(Id.createLinkId(186)));
    RoutingRequest toRequest3 = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(request3.getFromLink()),FacilitiesUtils.wrapLink(request3.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
    List<? extends PlanElement> request3Route = router.calcRoute(toRequest3);
    CarpoolingRequest request4 = new CarpoolingRequest(Id.create(4, Request.class),null,null,7*60*60,null,network.getLinks().get(Id.createLinkId(186)),network.getLinks().get(Id.createLinkId(1037)));
    RoutingRequest toRequest4 = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(request4.getFromLink()),FacilitiesUtils.wrapLink(request4.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
    List<? extends PlanElement> request4Route = router.calcRoute(toRequest4);
    CarpoolingRequest request5 = new CarpoolingRequest(Id.create(5, Request.class),null,null,8*60*60,null,network.getLinks().get(Id.createLinkId(688)),network.getLinks().get(Id.createLinkId(1540)));
    RoutingRequest toRequest5 = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(request5.getFromLink()),FacilitiesUtils.wrapLink(request5.getToLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
    List<? extends PlanElement> request5Route = router.calcRoute(toRequest5);
    HashMap<CarpoolingRequest, List<? extends PlanElement>> filteredPassengersRequests = new HashMap<>();

    @Test
    void noFilteredRequests(){
        assertNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
    }

    @Test
    void noRequestsLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,request4Route);
        filteredPassengersRequests.put(request5,request5Route);
        assertNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
    }

    @Test
    void oneRequestLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,request4Route);
        filteredPassengersRequests.put(request5,request5Route);
        filteredPassengersRequests.put(request3,request3Route);
        assertNotNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
        assertEquals("3",bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests).getId().toString());
    }

    @Test
    void moreThanOneRequestLessThanDetourFactorThreshold(){
        filteredPassengersRequests.put(request4,request4Route);
        filteredPassengersRequests.put(request5,request5Route);
        filteredPassengersRequests.put(request3,request3Route);
        filteredPassengersRequests.put(request2,request2Route);
        assertNotNull(bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests));
        assertEquals("2",bestRequestFinder.findBestRequest(driverRequest,filteredPassengersRequests).getId().toString());
    }
}