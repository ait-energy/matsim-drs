package at.ac.ait.matsim.drs.optimizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DrsOptimizer {
        private static final Logger LOGGER = LogManager.getLogger();
        private final Network network;
        private final DrsConfigGroup drsConfig;
        private final Population population;
        private final RoutingModule driverRouter;

        public DrsOptimizer(Network network,
                        DrsConfigGroup drsConfig,
                        Population population,
                        RoutingModule driverRouter) {
                this.network = network;
                this.drsConfig = drsConfig;
                this.population = population;
                this.driverRouter = driverRouter;
        }

        public MatchingResult optimize() {
                LOGGER.info("Matching process started!");
                ZoneSystem zoneSystem = new SquareGridZoneSystem(network, drsConfig.getCellSize());
                RequestZoneRegistry originZoneRegistry = RequestZoneRegistry.createRequestZoneRegistry(zoneSystem,
                                true);
                RequestZoneRegistry destinationZoneRegistry = RequestZoneRegistry.createRequestZoneRegistry(
                                zoneSystem,
                                false);
                RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(drsConfig);
                RequestsCollector requestsCollector = new RequestsCollector(drsConfig, population, network,
                                driverRouter);
                RequestsRegister requestsRegister = new RequestsRegister(originZoneRegistry, destinationZoneRegistry,
                                timeSegmentRegistry);
                PotentialRequestsFinder potentialRequestsFinder = new PotentialRequestsFinder(drsConfig,
                                requestsRegister);
                RequestsFilter requestsFilter = new RequestsFilter(drsConfig, driverRouter);
                BestRequestFinder bestRequestFinder = new BestRequestFinder(driverRouter);
                MatchMaker matchMaker = new MatchMaker(requestsCollector, requestsRegister, potentialRequestsFinder,
                                requestsFilter, bestRequestFinder);
                matchMaker.match();
                return matchMaker.getResult();
        }

}
