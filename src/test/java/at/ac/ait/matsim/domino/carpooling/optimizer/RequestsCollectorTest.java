package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;

import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestsCollectorTest {
    static Population population, populationWithZeroCarpoolingDrivers;
    static Network network;
    RequestsCollector requestsCollector, requestsCollectorNoRequests;

    @BeforeAll
    static void setup() {
        population = PopulationUtils.readPopulation("data/floridsdorf/population_carpooling.xml");
        populationWithZeroCarpoolingDrivers = PopulationUtils.readPopulation("data/floridsdorf/population.xml");
        network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");
        CarpoolingUtil.addNewAllowedModeToCarLinks(network,Carpooling.DRIVER_MODE);
    }

    @BeforeEach
    public void beforeEach() {
        requestsCollector = new RequestsCollector(population, network);
        requestsCollectorNoRequests = new RequestsCollector(populationWithZeroCarpoolingDrivers,
                NetworkUtils.createNetwork());
    }

    @Test
    void testNumberOfRequests(){
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        List<CarpoolingRequest> ridersRequests =requestsCollector.getRidersRequests();
        assertEquals(6,driversRequests.size());
        assertEquals(8,ridersRequests.size());
    }

    @Test
    void testRequestsInfo(){
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        List<CarpoolingRequest> ridersRequests =requestsCollector.getRidersRequests();

        assertEquals("1",driversRequests.get(0).getId().toString());
        assertEquals(5*60*60 ,driversRequests.get(0).getDepartureTime());
        assertEquals(Carpooling.DRIVER_MODE, driversRequests.get(0).getMode());
        assertEquals("1540" ,driversRequests.get(0).getFromLink().getId().toString());
        assertEquals("688",driversRequests.get(0).getToLink().getId().toString());

        assertEquals("1",ridersRequests.get(0).getId().toString());
        assertEquals(18600,ridersRequests.get(0).getDepartureTime());
        assertEquals(Carpooling.RIDER_MODE, ridersRequests.get(0).getMode());
        assertEquals("1541" ,ridersRequests.get(0).getFromLink().getId().toString());
        assertEquals("688",ridersRequests.get(0).getToLink().getId().toString());

    }

    @Test
    void testNoCarpoolingRequests(){
        requestsCollectorNoRequests.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollectorNoRequests.getDriversRequests();
        List<CarpoolingRequest> ridersRequests =requestsCollectorNoRequests.getRidersRequests();
        assertTrue(driversRequests.isEmpty());
        assertTrue(ridersRequests.isEmpty());
    }
}