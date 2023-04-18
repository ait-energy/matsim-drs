package at.ac.ait.matsim.domino.carpooling.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.ac.ait.matsim.domino.carpooling.analysis.CarpoolTripsInfoCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.RoutingModule;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

public class CarpoolingOptimizer {
        private static final Logger LOGGER = LogManager.getLogger();
        private final Network network;
        private final CarpoolingConfigGroup cfgGroup;
        private final Population population;
        private final RoutingModule router;
        private final Boolean isLastIteration;
        private final OutputDirectoryHierarchy outputDirectoryHierarchy;

        public CarpoolingOptimizer(Network network, CarpoolingConfigGroup cfgGroup, Population population,
                        RoutingModule router, boolean isLastIteration,
                        OutputDirectoryHierarchy outputDirectoryHierarchy) {
                this.network = network;
                this.cfgGroup = cfgGroup;
                this.population = population;
                this.router = router;
                this.isLastIteration = isLastIteration;
                this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        }

        public HashMap<CarpoolingRequest, CarpoolingRequest> optimize() {
                LOGGER.info("Matching process started!");
                ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(), cfgGroup.getCellSize());
                RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,
                                true);
                RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(
                                zonalSystem,
                                false);
                RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(cfgGroup);
                RequestsCollector requestsCollector = new RequestsCollector(population, network);
                RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry, destinationZonalRegistry,
                                timeSegmentRegistry);
                PotentialRequestsFinder potentialRequestsFinder = new PotentialRequestsFinder(cfgGroup,
                                requestsRegister);
                RequestsFilter requestsFilter = new RequestsFilter(cfgGroup, router);
                BestRequestFinder bestRequestFinder = new BestRequestFinder(router);
                HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
                List<CarpoolingRequest> driversRequests = new ArrayList<>();
                List<CarpoolingRequest> ridersRequests = new ArrayList<>();
                List<CarpoolingRequest> unmatchedDriversRequests = new ArrayList<>();
                List<CarpoolingRequest> unmatchedRidersRequests = new ArrayList<>();
                MatchMaker matchMaker = new MatchMaker(requestsCollector, requestsRegister, potentialRequestsFinder,
                                requestsFilter, bestRequestFinder, matchedRequests, driversRequests, ridersRequests,
                                unmatchedDriversRequests, unmatchedRidersRequests);
                matchMaker.match();
                HashMap<CarpoolingRequest, CarpoolingRequest> matchMap = matchMaker.getMatchedRequests();
                if (isLastIteration) {
                        CarpoolTripsInfoCollector infoCollector = new CarpoolTripsInfoCollector(
                                        outputDirectoryHierarchy);
                        infoCollector.printMatchedRequestsToCsv(matchMap);
                        infoCollector.printUnMatchedRequestsToCsv(matchMaker.getUnmatchedDriverRequests(),
                                        matchMaker.getUnmatchedRiderRequests());
                }
                return matchMap;
        }

}
