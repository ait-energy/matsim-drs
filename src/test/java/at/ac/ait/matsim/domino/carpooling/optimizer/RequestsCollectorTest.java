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
        CarpoolingUtil.addNewAllowedModeToCarLinks(network, Carpooling.DRIVER_MODE);
    }

    @BeforeEach
    public void beforeEach() {
        requestsCollector = new RequestsCollector(population, network);
        requestsCollectorNoRequests = new RequestsCollector(populationWithZeroCarpoolingDrivers,
                NetworkUtils.createNetwork());
    }

    @Test
    void testNumberOfRequests() {
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driverRequests = requestsCollector.getDriverRequests();
        List<CarpoolingRequest> riderRequests = requestsCollector.getRiderRequests();
        assertEquals(6, driverRequests.size());
        assertEquals(8, riderRequests.size());
    }

    @Test
    void testRequestsInfo() {
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driverRequests = requestsCollector.getDriverRequests();
        List<CarpoolingRequest> riderRequests = requestsCollector.getRiderRequests();

        assertEquals("1", driverRequests.get(0).getId().toString());
        assertEquals(5 * 60 * 60, driverRequests.get(0).getDepartureTime());
        assertEquals(Carpooling.DRIVER_MODE, driverRequests.get(0).getMode());
        assertEquals("1540", driverRequests.get(0).getFromLink().getId().toString());
        assertEquals("688", driverRequests.get(0).getToLink().getId().toString());

        assertEquals("1", riderRequests.get(0).getId().toString());
        assertEquals(18600, riderRequests.get(0).getDepartureTime());
        assertEquals(Carpooling.RIDER_MODE, riderRequests.get(0).getMode());
        assertEquals("1541", riderRequests.get(0).getFromLink().getId().toString());
        assertEquals("688", riderRequests.get(0).getToLink().getId().toString());

    }

    @Test
    void testNoCarpoolingRequests() {
        requestsCollectorNoRequests.collectRequests();
        List<CarpoolingRequest> driverRequests = requestsCollectorNoRequests.getDriverRequests();
        List<CarpoolingRequest> riderRequests = requestsCollectorNoRequests.getRiderRequests();
        assertTrue(driverRequests.isEmpty());
        assertTrue(riderRequests.isEmpty());
    }
}