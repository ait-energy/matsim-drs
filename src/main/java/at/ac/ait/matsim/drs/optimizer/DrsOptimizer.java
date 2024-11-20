package at.ac.ait.matsim.drs.optimizer;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.drs.analysis.DrsTripsInfoCollector;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DrsOptimizer {
        private static final Logger LOGGER = LogManager.getLogger();
        private final Network network;
        private final GlobalConfigGroup globalConfig;
        private final DrsConfigGroup drsConfig;
        private final Population population;
        private final RoutingModule driverRouter;
        private final Boolean isLastIteration;
        private final OutputDirectoryHierarchy outputDirectoryHierarchy;

        public DrsOptimizer(Network network,
                        GlobalConfigGroup globalConfig, DrsConfigGroup drsConfig,
                        Population population,
                        RoutingModule driverRouter, boolean isLastIteration,
                        OutputDirectoryHierarchy outputDirectoryHierarchy) {
                this.network = network;
                this.globalConfig = globalConfig;
                this.drsConfig = drsConfig;
                this.population = population;
                this.driverRouter = driverRouter;
                this.isLastIteration = isLastIteration;
                this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        }

        public List<DrsMatch> optimize() {
                LOGGER.info("Matching process started!");
                ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(), drsConfig.getCellSize());
                RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,
                                true);
                RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(
                                zonalSystem,
                                false);
                RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(drsConfig);
                RequestsCollector requestsCollector = new RequestsCollector(drsConfig, population, network,
                                driverRouter);
                RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry, destinationZonalRegistry,
                                timeSegmentRegistry);
                PotentialRequestsFinder potentialRequestsFinder = new PotentialRequestsFinder(drsConfig,
                                requestsRegister);
                RequestsFilter requestsFilter = new RequestsFilter(drsConfig, driverRouter);
                BestRequestFinder bestRequestFinder = new BestRequestFinder(driverRouter);
                MatchMaker matchMaker = new MatchMaker(requestsCollector, requestsRegister, potentialRequestsFinder,
                                requestsFilter, bestRequestFinder);
                matchMaker.match();
                List<DrsMatch> matches = matchMaker.getMatches();
                if (isLastIteration) {
                        DrsTripsInfoCollector infoCollector = new DrsTripsInfoCollector(globalConfig,
                                        outputDirectoryHierarchy);
                        infoCollector.printMatchedRequestsToCsv(matches);
                        infoCollector.printUnMatchedRequestsToCsv(matchMaker.getUnmatchedDriverRequests(),
                                        matchMaker.getUnmatchedRiderRequests());
                }
                return matches;
        }

}
