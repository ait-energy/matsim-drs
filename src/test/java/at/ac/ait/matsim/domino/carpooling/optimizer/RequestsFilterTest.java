package at.ac.ait.matsim.domino.carpooling.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

class RequestsFilterTest {
    static Network network;
    static RequestsFilter requestsFilter;
    static CarpoolingRequest driverRequest, request2, request3, request4, request5, request6;
    List<CarpoolingRequest> passengersRequests = new ArrayList<>();

    @BeforeAll
    static void setup() {
        network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");
        CarpoolingUtil.addNewAllowedModeToCarLinks(network,Carpooling.DRIVER_MODE);

        LeastCostPathCalculator dijkstra = new SpeedyDijkstra(new SpeedyGraph(network), new FreeSpeedTravelTime(),
                new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
        RoutingModule router = new NetworkRoutingModule(Carpooling.DRIVER_MODE, PopulationUtils.getFactory(), network,
                dijkstra);
        CarpoolingConfigGroup cfg =new CarpoolingConfigGroup("cfgGroup");
        cfg.riderDepartureTimeAdjustment=  0.05*60*60;
        requestsFilter = new RequestsFilter(cfg, router);
        driverRequest = new CarpoolingRequest(Id.create(1, Request.class), null, null, 8 * 60 * 60,
                null, network.getLinks().get(Id.createLinkId(1540)), null);
        request2 = new CarpoolingRequest(Id.create(2, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1674)), null);
        request3 = new CarpoolingRequest( Id.create(3, Request.class), null, null, 11 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1540)), null);
        request4 = new CarpoolingRequest( Id.create(4, Request.class), null, null, 7 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1540)), null);
        request5 = new CarpoolingRequest(Id.create(5, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1540)), null);
        request6 = new CarpoolingRequest(Id.create(6, Request.class), null, null, 8 * 60 * 60, null,
                network.getLinks().get(Id.createLinkId(1037)), null);
    }

    @BeforeEach
    public void beforeEach() {
        passengersRequests = new ArrayList<>();
    }

    @Test
    void testDriverArrivesTooLateToPassengerDueToDistance(){
        passengersRequests.add(request2);
        assertEquals(0,requestsFilter.filterRequests(driverRequest,passengersRequests).size());
    }

    @Test
    void testDriverArrivesTooLateToPassengerDueToTime(){
        passengersRequests.add(request3);
        assertEquals(0,requestsFilter.filterRequests(driverRequest,passengersRequests).size());
    }

    @Test
    void testDriverArrivesTooEarlyToPassengerDueToTime(){
        passengersRequests.add(request4);
        assertEquals(0,requestsFilter.filterRequests(driverRequest,passengersRequests).size());
    }

    @Test
    void testDriverArrivesOnTime(){
        passengersRequests.add(request5);
        passengersRequests.add(request6);
        assertEquals(2,requestsFilter.filterRequests(driverRequest,passengersRequests).size());
    }
}