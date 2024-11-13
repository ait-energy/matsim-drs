package at.ac.ait.matsim.drs.planHandler;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import at.ac.ait.matsim.drs.optimizer.DrsOptimizer;
import at.ac.ait.matsim.drs.request.DrsMatch;
import at.ac.ait.matsim.drs.request.DrsRequest;
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
    private final DrsConfigGroup cfgGroup;
    private final RoutingModule driverRouter, riderRouter;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    public PlanModifier(Scenario scenario, TripRouter tripRouter, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.scenario = scenario;
        this.drsNetwork = NetworkTools.createFilteredNetworkByLinkMode(scenario.getNetwork(),
                ImmutableSet.of(Drs.DRIVER_MODE));
        cfgGroup = Drs.addOrGetConfigGroup(scenario);
        driverRouter = tripRouter.getRoutingModule(Drs.DRIVER_MODE);
        riderRouter = tripRouter.getRoutingModule(Drs.RIDER_MODE);
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
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
    public void notifyReplanning(ReplanningEvent replanningEvent) {
        DrsUtil.routeCalculations.set(0);
        preplanDay(replanningEvent.isLastIteration());
        LOGGER.info("plan modifier used {} route calculations.", DrsUtil.routeCalculations.get());
    }

    private void preplanDay(boolean isLastIteration) {
        Population population = scenario.getPopulation();
        DrsOptimizer optimizer = new DrsOptimizer(drsNetwork, cfgGroup, population,
                driverRouter, isLastIteration, outputDirectoryHierarchy);
        List<DrsMatch> matches = optimizer.optimize();
        PopulationFactory populationFactory = population.getFactory();
        LOGGER.info("Modifying drs agents plans started.");
        for (DrsMatch match : matches) {
            modifyPlans(match, populationFactory);
        }
        addRoutingModeAndRouteForRiders();
        LOGGER.info("Modifying drs agents plans finished.");
    }

    private void modifyPlans(DrsMatch match, PopulationFactory factory) {
        double pickupTime = match.getDriver().getDepartureTime() + match.getToPickup().getTravelTime().seconds();
        addNewActivitiesToDriverPlan(match, pickupTime, factory);
        adjustRiderDepartureTime(match.getRider(), pickupTime);
    }

    private void addNewActivitiesToDriverPlan(DrsMatch match, double pickupTime, PopulationFactory factory) {
        Activity pickup = factory.createActivityFromLinkId(Drs.DRIVER_INTERACTION,
                match.getRider().getFromLink().getId());
        pickup.setEndTime(pickupTime + cfgGroup.getPickupWaitingSeconds());
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

    static void adjustRiderDepartureTime(DrsRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    if (!(DrsUtil.getLinkageActivityToRiderRequest((Activity) planElement) == null)) {
                        if (DrsUtil.getLinkageActivityToRiderRequest((Activity) planElement).equals(riderRequest
                                .getId().toString())) {
                            DrsUtil.setActivityOriginalDepartureTime((Activity) planElement,
                                    riderRequest.getDepartureTime());
                            LOGGER.debug("Before matching " + riderRequest.getPerson().getId().toString()
                                    + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                            ((Activity) planElement).setEndTime(pickupTime);
                            DrsUtil.setLinkageActivityToRiderRequest((Activity) planElement, null);
                            LOGGER.debug("After matching " + riderRequest.getPerson().getId().toString()
                                    + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                            break;
                            // TODO this only adjusts one element. the following elements must be adjusted
                            // as well otherwise the following elements may have a start time before the end
                            // time of the adjusted element.
                        }
                    }
                }
            }
        }
    }

    /**
     * TODO this should be integrated into the optimizer/matchmaker process
     *
     * for all rider legs: add a routing mode and a generic route (that is provided
     * through the drs rider mode configured as teleporting mode)
     * so that PersonPrepareForSim does not reroute our legs (and thereby discards
     * our leg attributes where we store if the person found a match or not)
     */
    private void addRoutingModeAndRouteForRiders() {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            for (int i = 0; i < planElements.size(); i++) {
                if (planElements.get(i) instanceof Leg) {
                    Leg leg = (Leg) planElements.get(i);
                    if (!leg.getMode().equals(Drs.RIDER_MODE)) {
                        continue;
                    }
                    TripStructureUtils.setRoutingMode(leg, Drs.RIDER_MODE);

                    Activity prevActivity = (Activity) planElements.get(i - 1);
                    Activity nextActivity = (Activity) planElements.get(i + 1);
                    Leg calculatedLeg = DrsUtil.calculateLeg(riderRouter,
                            FacilitiesUtils.wrapActivity(prevActivity),
                            FacilitiesUtils.wrapActivity(nextActivity),
                            leg.getDepartureTime().orElse(0),
                            person);
                    leg.setRoute(calculatedLeg.getRoute());
                }
            }
        }
    }

}
