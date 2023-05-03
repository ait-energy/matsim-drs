package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.domino.carpooling.analysis.CarpoolTripsInfoCollector;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingMatch;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

public class CarpoolingOptimizer {
        private static final Logger LOGGER = LogManager.getLogger();
        private final Network network;
        private final CarpoolingConfigGroup cfgGroup;
        private final Population population;
        private final RoutingModule driverRouter;
        private final Boolean isLastIteration;
        private final OutputDirectoryHierarchy outputDirectoryHierarchy;

        public CarpoolingOptimizer(Network network, CarpoolingConfigGroup cfgGroup, Population population,
                        RoutingModule driverRouter, boolean isLastIteration,
                        OutputDirectoryHierarchy outputDirectoryHierarchy) {
                this.network = network;
                this.cfgGroup = cfgGroup;
                this.population = population;
                this.driverRouter = driverRouter;
                this.isLastIteration = isLastIteration;
                this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        }

        public List<CarpoolingMatch> optimize() {
                LOGGER.info("Matching process started!");
                ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(), cfgGroup.getCellSize());
                RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,
                                true);
                RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(
                                zonalSystem,
                                false);
                RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(cfgGroup);
                RequestsCollector requestsCollector = new RequestsCollector(cfgGroup, population, network,
                                driverRouter);
                RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry, destinationZonalRegistry,
                                timeSegmentRegistry);
                PotentialRequestsFinder potentialRequestsFinder = new PotentialRequestsFinder(cfgGroup,
                                requestsRegister);
                RequestsFilter requestsFilter = new RequestsFilter(cfgGroup, driverRouter);
                BestRequestFinder bestRequestFinder = new BestRequestFinder(driverRouter);
                MatchMaker matchMaker = new MatchMaker(requestsCollector, requestsRegister, potentialRequestsFinder,
                                requestsFilter, bestRequestFinder);
                matchMaker.match();
                List<CarpoolingMatch> matches = matchMaker.getMatches();
                if (isLastIteration) {
                        CarpoolTripsInfoCollector infoCollector = new CarpoolTripsInfoCollector(
                                        outputDirectoryHierarchy);
                        infoCollector.printMatchedRequestsToCsv(matches);
                        infoCollector.printUnMatchedRequestsToCsv(matchMaker.getUnmatchedDriverRequests(),
                                        matchMaker.getUnmatchedRiderRequests());
                }
                return matches;
        }

}
