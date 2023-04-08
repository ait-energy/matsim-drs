package at.ac.ait.matsim.domino.carpooling.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;

import java.util.List;

import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.FacilitiesUtils;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;

class BestRequestFinderTest {
    static Network network;
    static CarpoolingRequest driverRequest, request2, request3, request4, request5;
    static List<? extends PlanElement> request2Route, request3Route, request4Route, request5Route;
    static RoutingRequest toRequest2, toRequest3, toRequest4, toRequest5;
    static BestRequestFinder bestRequestFinder;
    List<CarpoolingRequest> filteredPassengersRequests;

    @BeforeAll
    static void setup() {
        network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");
        CarpoolingUtil.addNewAllowedModeToCarLinks(network, Carpooling.DRIVER_MODE);
        LeastCostPathCalculator dijkstra = new SpeedyDijkstra(new SpeedyGraph(network),
                new FreeSpeedTravelTime(),
                new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
        RoutingModule router = new NetworkRoutingModule(Carpooling.DRIVER_MODE, PopulationUtils.getFactory(),
                network,
                dijkstra);
        bestRequestFinder = new BestRequestFinder(router);

        driverRequest = new CarpoolingRequest(Id.create(1, Request.class), null, null, 8 * 60 * 60,
                null, network.getLinks().get(Id.createLinkId(1540)),
                network.getLinks().get(Id.createLinkId(186)), null);

        request2 = new CarpoolingRequest(Id.create(2, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1541)),
                network.getLinks().get(Id.createLinkId(186)), null);
        toRequest2 = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(request2.getFromLink()),
                FacilitiesUtils.wrapLink(request2.getToLink()),
                driverRequest.getDepartureTime(), driverRequest.getPerson());
        request2Route = router.calcRoute(toRequest2);

        request3 = new CarpoolingRequest(Id.create(3, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1037)),
                network.getLinks().get(Id.createLinkId(186)), null);
        toRequest3 = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(request3.getFromLink()),
                FacilitiesUtils.wrapLink(request3.getToLink()),
                driverRequest.getDepartureTime(), driverRequest.getPerson());
        request3Route = router.calcRoute(toRequest3);

        request4 = new CarpoolingRequest(Id.create(4, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(186)),
                network.getLinks().get(Id.createLinkId(1037)), null);
        toRequest4 = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(request4.getFromLink()),
                FacilitiesUtils.wrapLink(request4.getToLink()),
                driverRequest.getDepartureTime(), driverRequest.getPerson());
        request4Route = router.calcRoute(toRequest4);

        request5 = new CarpoolingRequest(Id.create(5, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(688)),
                network.getLinks().get(Id.createLinkId(1540)), null);
        toRequest5 = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(request5.getFromLink()),
                FacilitiesUtils.wrapLink(request5.getToLink()),
                driverRequest.getDepartureTime(), driverRequest.getPerson());
        request5Route = router.calcRoute(toRequest5);
    }

    @BeforeEach
    public void beforeEach() {
        filteredPassengersRequests = new ArrayList<>();
    }

    @Test
    void noFilteredRequestsTest() {
        assertNull(bestRequestFinder.findBestRequest(driverRequest, filteredPassengersRequests));
    }

    @Test
    void findBestRequestTest() {
        filteredPassengersRequests.add(request4);
        filteredPassengersRequests.add(request5);
        filteredPassengersRequests.add(request3);
        filteredPassengersRequests.add(request2);
        assertNotNull(bestRequestFinder.findBestRequest(driverRequest, filteredPassengersRequests));
        assertEquals("2", bestRequestFinder.findBestRequest(driverRequest, filteredPassengersRequests).getId()
                .toString());
    }
}