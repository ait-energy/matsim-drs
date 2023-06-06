package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.drs.RoutingForTests;
import at.ac.ait.matsim.drs.request.CarpoolingRequest;
import at.ac.ait.matsim.drs.run.Carpooling;
import at.ac.ait.matsim.drs.run.CarpoolingConfigGroup;

class RequestsCollectorTest {
    private static Population population, populationWithZeroCarpoolingDrivers;
    private static Network network;
    private static RoutingModule driverRouter;

    @BeforeAll
    static void setup() {
        // population with predefined driver/rider legs (see length in xml comments)
        population = PopulationUtils.readPopulation("data/floridsdorf/population_carpooling.xml");
        populationWithZeroCarpoolingDrivers = PopulationUtils.readPopulation("data/floridsdorf/population.xml");

        RoutingForTests routingForTests = new RoutingForTests("data/floridsdorf/network.xml");
        network = routingForTests.getNetwork();
        driverRouter = routingForTests.getDriverRouter();
    }

    @Test
    void testNumberOfRequests() {
        RequestsCollector collector = new RequestsCollector(new CarpoolingConfigGroup(), population, network,
                driverRouter);
        collector.collectRequests();

        assertEquals(6, collector.getDriverRequests().size());
        assertEquals(8, collector.getRiderRequests().size());
    }

    @Test
    void testNumberOfRequestsWithMinimumDistances() {
        CarpoolingConfigGroup cfgWithMinimums = new CarpoolingConfigGroup();
        cfgWithMinimums.setMinDriverLegMeters(4_500);
        cfgWithMinimums.setMinRiderLegMeters(2_000);
        RequestsCollector collector = new RequestsCollector(cfgWithMinimums, population, network, driverRouter);
        collector.collectRequests();

        assertEquals(4, collector.getDriverRequests().size());
        assertEquals(6, collector.getRiderRequests().size());
    }

    @Test
    void testNumberOfRequestsWithMinimumDistancesExtreme() {
        CarpoolingConfigGroup cfgWithMinimums = new CarpoolingConfigGroup();
        cfgWithMinimums.setMinDriverLegMeters(20_000);
        cfgWithMinimums.setMinRiderLegMeters(20_000);
        RequestsCollector collector = new RequestsCollector(cfgWithMinimums, population, network, driverRouter);
        collector.collectRequests();

        assertTrue(collector.getDriverRequests().isEmpty());
        assertTrue(collector.getRiderRequests().isEmpty());
    }

    @Test
    void testRequestsInfo() {
        RequestsCollector collector = new RequestsCollector(new CarpoolingConfigGroup(), population, network,
                driverRouter);
        collector.collectRequests();
        List<CarpoolingRequest> driverRequests = collector.getDriverRequests();
        List<CarpoolingRequest> riderRequests = collector.getRiderRequests();

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
        RequestsCollector collector = new RequestsCollector(new CarpoolingConfigGroup(),
                populationWithZeroCarpoolingDrivers, NetworkUtils.createNetwork(), driverRouter);
        collector.collectRequests();
        List<CarpoolingRequest> driverRequests = collector.getDriverRequests();
        List<CarpoolingRequest> riderRequests = collector.getRiderRequests();
        assertTrue(driverRequests.isEmpty());
        assertTrue(riderRequests.isEmpty());
    }
}