package at.ac.ait.matsim.drs.engine;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.replanning.conflicts.ConflictManager;
import org.matsim.core.replanning.conflicts.ConflictWriter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import at.ac.ait.matsim.drs.analysis.DrsTripsInfoCollector;
import at.ac.ait.matsim.drs.optimizer.DrsMatch;
import at.ac.ait.matsim.drs.optimizer.DrsOptimizer;
import at.ac.ait.matsim.drs.optimizer.DrsRequest;
import at.ac.ait.matsim.drs.optimizer.MatchingResult;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Note: PlanModifier should be called after PersonPrepareForSim is
 * finished, otherwise the leg attributes (where we store machting info) can be
 * cleared due to rerouting
 */
public class PlanModifier implements ReplanningListener, IterationStartsListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Scenario scenario;
    private final Network drsNetwork;
    private final GlobalConfigGroup globalConfig;
    private final DrsConfigGroup drsConfig;
    private final RoutingModule driverRouter;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final ConflictManager conflictManager;
    private final UnmatchedRiderConflictResolver unmatchedRiderConflictResolver;

    @Inject
    public PlanModifier(Scenario scenario, TripRouter tripRouter, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.scenario = scenario;
        this.drsNetwork = NetworkTools.createFilteredNetworkByLinkMode(scenario.getNetwork(),
                ImmutableSet.of(Drs.DRIVER_MODE));
        this.globalConfig = scenario.getConfig().global();
        this.drsConfig = Drs.addOrGetConfigGroup(scenario);
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

    /** before iteration 0 */
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() != 0) {
            return;
        }
        DrsUtil.routeCalculations.set(0);
        preplanDay(event.isLastIteration());
        LOGGER.info("plan modifier used {} route calculations.", DrsUtil.routeCalculations.get());
    }

    /** before iterations > 0 */
    @Override
    public void notifyReplanning(ReplanningEvent event) {
        DrsUtil.routeCalculations.set(0);
        conflictManager.initializeReplanning(scenario.getPopulation());
        preplanDay(event.isLastIteration());
        conflictManager.run(scenario.getPopulation(), event.getIteration());
        unmatchedRiderConflictResolver.deleteInvalidPlans(scenario.getPopulation());
        LOGGER.info("plan modifier used {} route calculations.", DrsUtil.routeCalculations.get());
    }

    private void preplanDay(boolean isLastIteration) {
        Population population = scenario.getPopulation();
        DrsOptimizer optimizer = new DrsOptimizer(drsNetwork, drsConfig, population, driverRouter);
        MatchingResult result = optimizer.optimize();
        if (isLastIteration) {
            DrsTripsInfoCollector infoCollector = new DrsTripsInfoCollector(globalConfig,
                    outputDirectoryHierarchy);
            infoCollector.printMatchedRequestsToCsv(result.matches());
            infoCollector.printUnMatchedRequestsToCsv(result.unmatchedDriverRequests(),
                    result.unmatchedRiderRequests());
        }

        PopulationFactory populationFactory = population.getFactory();
        LOGGER.info("Modifying drs agents plans started.");
        for (DrsMatch match : result.matches()) {
            modifyPlans(match, populationFactory);
        }
        // addRoutingModeAndRouteForRiders();
        LOGGER.info("Modifying drs agents plans finished.");
    }

    private void modifyPlans(DrsMatch match, PopulationFactory factory) {
        DrsUtil.setAssignedDriver(match.getRider().getDrsLeg(), match.getDriver().getPerson().getId().toString());
        double pickupTime = match.getDriver().getDepartureTime() + match.getToPickup().getTravelTime().seconds();
        addNewActivitiesToDriverPlan(match, pickupTime, factory);
        addRiderRoute(match.getRider());
        adjustRiderDepartureTime(match.getRider(), pickupTime);
    }

    private void addNewActivitiesToDriverPlan(DrsMatch match, double pickupTime, PopulationFactory factory) {
        Activity pickup = factory.createActivityFromLinkId(Drs.DRIVER_INTERACTION,
                match.getRider().getFromLink().getId());
        pickup.setEndTime(pickupTime + drsConfig.getPickupWaitingSeconds());
        DrsUtil.setActivityType(pickup, Drs.ActivityType.pickup);
        DrsUtil.setRiderId(pickup, match.getRider().getPerson().getId());

        double dropoffTime = pickupTime + match.getWithCustomer().getTravelTime().seconds();
        Activity dropoff = factory.createActivityFromLinkId(Drs.DRIVER_INTERACTION,
                match.getRider().getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        DrsUtil.setActivityType(dropoff, Drs.ActivityType.dropoff);
        DrsUtil.setRiderId(dropoff, match.getRider().getPerson().getId());

        List<PlanElement> newTrip = Arrays.asList(match.getToPickup(), pickup, match.getWithCustomer(), dropoff,
                match.getAfterDropoff());
        DrsUtil.setRoutingModeToDriver(newTrip);
        Activity startActivity = match.getDriver().getTrip().getOriginActivity();
        Activity endActivity = match.getDriver().getTrip().getDestinationActivity();
        List<PlanElement> planElements = match.getDriver().getPerson().getSelectedPlan().getPlanElements();
        LOGGER.debug("Before matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
        TripRouter.insertTrip(planElements, startActivity, newTrip, endActivity);
        LOGGER.debug("After matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
    }

    /**
     * For rider legs it is important to set the routing mode and a generic route
     * so that PersonPrepareForSim does not reroute our legs (and thereby discards
     * our leg attributes where we store if the person found a match or not)
     */
    private void addRiderRoute(DrsRequest riderRequest) {
        Route genericRoute = new GenericRouteImpl(riderRequest.getFromLink().getId(),
                riderRequest.getToLink().getId());
        genericRoute.setDistance(riderRequest.getNetworkRouteDistance());
        genericRoute.setTravelTime(riderRequest.getNetworkRouteTravelTime().seconds());

        Leg leg = riderRequest.getDrsLeg();
        leg.setRoute(genericRoute);
        TripStructureUtils.setRoutingMode(leg, Drs.RIDER_MODE);
    }

    /**
     * Note: only adjusts one activity!
     * It may happen that later activities start before the end of the adjusted
     * activity.
     *
     * @param riderRequest
     * @param pickupTime
     */
    static void adjustRiderDepartureTime(DrsRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    Activity act = (Activity) planElement;
                    if (act.getEndTime().isUndefined()) {
                        continue;
                    }
                    int endTime = (int) act.getEndTime().seconds();
                    if (endTime == (int) riderRequest.getDepartureTime()) {
                        DrsUtil.setActivityOriginalDepartureTime((Activity) planElement,
                                riderRequest.getDepartureTime());
                        LOGGER.debug("Before matching " + riderRequest.getPerson().getId().toString()
                                + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                        ((Activity) planElement).setEndTime(pickupTime);
                        LOGGER.debug("After matching " + riderRequest.getPerson().getId().toString()
                                + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                        break;
                    }
                }
            }
        }
    }

}
