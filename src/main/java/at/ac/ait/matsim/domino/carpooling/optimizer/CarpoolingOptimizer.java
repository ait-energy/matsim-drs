package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.router.RoutingModule;

import java.util.HashMap;

public class CarpoolingOptimizer {

    Logger LOGGER = LogManager.getLogger();
    private final Network network;
    private final CarpoolingConfigGroup cfgGroup;
    private final Population population;
    private final RoutingModule router;
    private final Integer iterationNumber;

    public CarpoolingOptimizer(Network network, CarpoolingConfigGroup cfgGroup, Population population, RoutingModule router, Integer iterationNumber) {
        this.network = network;
        this.cfgGroup = cfgGroup;
        this.population = population;
        this.router = router;
        this.iterationNumber = iterationNumber;
    }


    public HashMap<CarpoolingRequest, CarpoolingRequest> optimize() {

        LOGGER.info("Matching process started!");
        ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(),cfgGroup.cellSize);
        RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,true);
        RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,false);
        RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(cfgGroup);

        RequestsCollector requestsCollector = new RequestsCollector(population,network);
        RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry,destinationZonalRegistry,timeSegmentRegistry);
        NearestRequestsFinder nearestRequestsFinder = new NearestRequestsFinder(cfgGroup,requestsRegister);
        RequestsFilter requestsFilter = new RequestsFilter(cfgGroup, router);
        BestRequestFinder bestRequestFinder = new BestRequestFinder(router,cfgGroup);
        MatchMaker matchMaker = new MatchMaker(requestsCollector, requestsRegister,nearestRequestsFinder, requestsFilter, bestRequestFinder, iterationNumber);
        return matchMaker.match();
    }

}
