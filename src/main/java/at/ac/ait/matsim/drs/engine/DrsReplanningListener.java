package at.ac.ait.matsim.drs.engine;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.conflicts.ConflictManager;
import org.matsim.core.replanning.conflicts.ConflictWriter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.analysis.DrsTripsInfoCollector;
import at.ac.ait.matsim.drs.optimizer.BestRequestFinder;
import at.ac.ait.matsim.drs.optimizer.DrsMatch;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.optimizer.MatchMaker;
import at.ac.ait.matsim.drs.optimizer.MatchingResult;
import at.ac.ait.matsim.drs.optimizer.RequestsCollector;
import at.ac.ait.matsim.drs.optimizer.RequestsFilter;
import at.ac.ait.matsim.drs.optimizer.RequestsRegister;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Entrypoint to trigger the drs optimization / arrangement in the replanning
 * phase.
 *
 * Note: this process must be started AFTER PersonPrepareForSim!
 * Otherwise the leg attributes (where we store machting info)
 * will be cleared due to rerouting.
 */
public class DrsReplanningListener implements ReplanningListener, IterationStartsListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Scenario scenario;
    private final DrsData drsData;
    private final GlobalConfigGroup globalConfig;
    private final DrsConfigGroup drsConfig;
    private final RoutingModule driverRouter;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final ConflictManager conflictManager;
    private final UnmatchedRiderConflictResolver unmatchedRiderConflictResolver;

    @Inject
    public DrsReplanningListener(Scenario scenario, DrsConfigGroup drsConfig, DrsData drsData, TripRouter tripRouter,
            OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.scenario = scenario;
        this.drsData = drsData;
        this.globalConfig = scenario.getConfig().global();
        this.drsConfig = drsConfig;
        driverRouter = tripRouter.getRoutingModule(Drs.DRIVER_MODE);
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;

        // Create a custom ConflictManager with an actual resolver for our conflicts.
        // Must not be bound to the "official" conflict resolver because
        // PlansReplanningImpl
        // runs the that directly after replanning,
        // which is before our PlanModifier can assign riders to drivers.
        this.unmatchedRiderConflictResolver = new UnmatchedRiderConflictResolver();
        ConflictWriter drsConflictWriter = new ConflictWriter(new File(
                outputDirectoryHierarchy.getOutputFilename("drs_conflicts.csv")));
        this.conflictManager = new ConflictManager(
                Set.of(unmatchedRiderConflictResolver),
                drsConflictWriter,
                MatsimRandom.getRandom());
    }

    @Override
    public double priority() {
        return 10;
    }

    /** before iteration 0 */
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() != 0) {
            return;
        }
        optimizeDrs(event.isLastIteration());
    }

    /** before iterations > 0 */
    @Override
    public void notifyReplanning(ReplanningEvent event) {
        conflictManager.initializeReplanning(scenario.getPopulation());
        optimizeDrs(event.isLastIteration());
        conflictManager.run(scenario.getPopulation(), event.getIteration());

        // TODO rethink this. deleting the plans here could hinder
        // reaching equilibrium (as seen with PerfectMatchExample)
        // unmatchedRiderConflictResolver.deleteInvalidPlans(scenario.getPopulation());
    }

    private void optimizeDrs(boolean isLastIteration) {
        LOGGER.info("Drs replanning started.");
        DrsUtil.routeCalculations.set(0);
        Population population = scenario.getPopulation();

        RequestsCollector requestsCollector = new RequestsCollector(drsConfig, population,
                drsData.getDrsNetwork(), driverRouter);
        requestsCollector.collectRequests();
        List<DrsDriverRequest> driverRequests = requestsCollector.getDriverRequests();
        List<DrsRiderRequest> riderRequests = requestsCollector.getRiderRequests();
        LOGGER.info("Collected {} {} and {} {} requests", driverRequests.size(), Drs.DRIVER_MODE,
                riderRequests.size(), Drs.RIDER_MODE);

        RequestsRegister requestsRegister = new RequestsRegister(drsConfig, drsData.getH3ZoneSystem());
        RequestsFilter requestsFilter = new RequestsFilter(drsConfig, driverRouter);
        BestRequestFinder bestRequestFinder = new BestRequestFinder(driverRouter);
        MatchMaker matchMaker = new MatchMaker(drsConfig, driverRequests, riderRequests, requestsRegister,
                requestsFilter, bestRequestFinder);
        MatchingResult result = matchMaker.match();
        LOGGER.info("Found {} drs matches.", result.matches().size());

        if (isLastIteration) {
            DrsTripsInfoCollector infoCollector = new DrsTripsInfoCollector(globalConfig,
                    outputDirectoryHierarchy);
            infoCollector.printMatchedRequestsToCsv(result.matches());
            infoCollector.printUnMatchedRequestsToCsv(result.unmatchedDriverRequests(),
                    result.unmatchedRiderRequests());
        }

        PlanModifier planModifier = new PlanModifier(drsConfig, population.getFactory());
        for (DrsMatch match : result.matches()) {
            planModifier.modifyPlans(match);
        }
        LOGGER.info("Modified drs plans.");
        LOGGER.info("Drs replanning finished using {} route calculations.",
                DrsUtil.routeCalculations.get());
    }

}
