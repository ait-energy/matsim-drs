package at.ac.ait.matsim.drs.optimizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.drs.engine.DrsData;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DrsOptimizer {
        private static final Logger LOGGER = LogManager.getLogger();
        private final DrsData drsData;
        private final DrsConfigGroup drsConfig;
        private final Population population;
        private final RoutingModule driverRouter;

        public DrsOptimizer(DrsData drsData,
                        DrsConfigGroup drsConfig,
                        Population population,
                        RoutingModule driverRouter) {
                this.drsData = drsData;
                this.drsConfig = drsConfig;
                this.population = population;
                this.driverRouter = driverRouter;
        }

        public MatchingResult optimize() {
                LOGGER.info("Matching process started!");
                RequestZoneRegistry originZoneRegistry = RequestZoneRegistry.createRequestZoneRegistry(
                                drsData.getZoneSystem(), true);
                RequestZoneRegistry destinationZoneRegistry = RequestZoneRegistry.createRequestZoneRegistry(
                                drsData.getZoneSystem(), false);
                RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(drsConfig);
                RequestsCollector requestsCollector = new RequestsCollector(drsConfig, population,
                                drsData.getDrsNetwork(), driverRouter);
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
