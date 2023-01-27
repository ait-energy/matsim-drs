package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestsCollectorTest {
    Population population = PopulationUtils.readPopulation("data/floridsdorf/population_carpooling.xml");
    Population populationWithZeroCarpoolingDrivers = PopulationUtils.readPopulation("data/floridsdorf/population.xml");
    Network network = NetworkUtils.readNetwork("data/floridsdorf/network_carpooling.xml");
    RequestsCollector requestsCollector = new RequestsCollector(population,network);
    RequestsCollector requestsCollectorNoRequests = new RequestsCollector(populationWithZeroCarpoolingDrivers,NetworkUtils.createNetwork());

    @Test
    void testNumberOfRequests(){
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        List<CarpoolingRequest> passengersRequests =requestsCollector.getPassengersRequests();
        assertEquals(6,driversRequests.size());
        assertEquals(8,passengersRequests.size());
    }

    @Test
    void testRequestsInfo(){
        requestsCollector.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollector.getDriversRequests();
        List<CarpoolingRequest> passengersRequests =requestsCollector.getPassengersRequests();

        assertEquals("1",driversRequests.get(0).getId().toString());
        assertEquals(5*60*60 ,driversRequests.get(0).getDepartureTime());
        assertEquals("carpoolingDriver",driversRequests.get(0).getMode());
        assertEquals("1511" ,driversRequests.get(0).getFromLink().getId().toString());
        assertEquals("1364",driversRequests.get(0).getToLink().getId().toString());

        assertEquals("1",passengersRequests.get(0).getId().toString());
        assertEquals(5*60*60,passengersRequests.get(0).getDepartureTime());
        assertEquals("carpoolingPassenger",passengersRequests.get(0).getMode());
        assertEquals("1533" ,passengersRequests.get(0).getFromLink().getId().toString());
        assertEquals("1143",passengersRequests.get(0).getToLink().getId().toString());

    }

    @Test
    void testNoCarpoolingRequests(){
        requestsCollectorNoRequests.collectRequests();
        List<CarpoolingRequest> driversRequests = requestsCollectorNoRequests.getDriversRequests();
        List<CarpoolingRequest> passengersRequests =requestsCollectorNoRequests.getPassengersRequests();
        assertTrue(driversRequests.isEmpty());
        assertTrue(passengersRequests.isEmpty());
    }
}