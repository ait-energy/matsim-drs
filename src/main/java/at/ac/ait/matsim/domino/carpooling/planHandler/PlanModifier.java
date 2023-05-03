package at.ac.ait.matsim.domino.carpooling.planHandler;

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
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import at.ac.ait.matsim.domino.carpooling.optimizer.CarpoolingOptimizer;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingMatch;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class PlanModifier implements ReplanningListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Scenario scenario;
    private final Network carpoolingNetwork;
    private final CarpoolingConfigGroup cfgGroup;
    private final RoutingModule driverRouter, riderRouter;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    public PlanModifier(Scenario scenario, TripRouter tripRouter, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.scenario = scenario;
        this.carpoolingNetwork = NetworkTools.createFilteredNetworkByLinkMode(scenario.getNetwork(),
                ImmutableSet.of(Carpooling.DRIVER_MODE));
        cfgGroup = Carpooling.addOrGetConfigGroup(scenario);
        driverRouter = tripRouter.getRoutingModule(Carpooling.DRIVER_MODE);
        riderRouter = tripRouter.getRoutingModule(Carpooling.RIDER_MODE);
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
    }

    @Override
    public void notifyReplanning(ReplanningEvent replanningEvent) {
        CarpoolingUtil.routeCalculations.set(0);
        preplanDay(replanningEvent);
        LOGGER.info("plan modifier used {} route calculations.", CarpoolingUtil.routeCalculations.get());
    }

    private void preplanDay(ReplanningEvent event) {
        Population population = scenario.getPopulation();
        CarpoolingOptimizer optimizer = new CarpoolingOptimizer(carpoolingNetwork, cfgGroup, population,
                driverRouter, event.isLastIteration(), outputDirectoryHierarchy);
        List<CarpoolingMatch> matches = optimizer.optimize();
        PopulationFactory populationFactory = population.getFactory();
        LOGGER.info("Modifying carpooling agents plans started.");
        for (CarpoolingMatch match : matches) {
            modifyPlans(match, populationFactory);
        }
        addRoutingModeAndRouteForRiders();
        LOGGER.info("Modifying carpooling agents plans finished.");
    }

    private void modifyPlans(CarpoolingMatch match, PopulationFactory factory) {
        double pickupTime = match.getDriver().getDepartureTime() + match.getToPickup().getTravelTime().seconds();
        addNewActivitiesToDriverPlan(match, pickupTime, factory);
        adjustRiderDepartureTime(match.getRider(), pickupTime);
    }

    private void addNewActivitiesToDriverPlan(CarpoolingMatch match, double pickupTime, PopulationFactory factory) {
        Activity pickup = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                match.getRider().getFromLink().getId());
        pickup.setEndTime(pickupTime + cfgGroup.getPickupWaitingSeconds());
        CarpoolingUtil.setActivityType(pickup, Carpooling.ActivityType.pickup);
        CarpoolingUtil.setRiderId(pickup, match.getRider().getPerson().getId());

        double dropoffTime = pickupTime + match.getWithCustomer().getTravelTime().seconds();
        Activity dropoff = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                match.getRider().getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        CarpoolingUtil.setActivityType(dropoff, Carpooling.ActivityType.dropoff);
        CarpoolingUtil.setRiderId(dropoff, match.getRider().getPerson().getId());

        List<PlanElement> newTrip = Arrays.asList(match.getToPickup(), pickup, match.getWithCustomer(), dropoff,
                match.getAfterDropoff());
        CarpoolingUtil.setRoutingModeToDriver(newTrip);
        Activity startActivity = match.getDriver().getTrip().getOriginActivity();
        Activity endActivity = match.getDriver().getTrip().getDestinationActivity();
        List<PlanElement> planElements = match.getDriver().getPerson().getSelectedPlan().getPlanElements();
        LOGGER.debug("Before matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
        TripRouter.insertTrip(planElements, startActivity, newTrip, endActivity);
        LOGGER.debug("After matching " + match.getDriver().getPerson().getId().toString() + " had "
                + match.getDriver().getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
    }

    static void adjustRiderDepartureTime(CarpoolingRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    if (!(CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement) == null)) {
                        if (CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement).equals(riderRequest
                                .getId().toString())) {
                            CarpoolingUtil.setActivityOriginalDepartureTime((Activity) planElement,
                                    riderRequest.getDepartureTime());
                            LOGGER.debug("Before matching " + riderRequest.getPerson().getId().toString()
                                    + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                            ((Activity) planElement).setEndTime(pickupTime);
                            CarpoolingUtil.setLinkageActivityToRiderRequest((Activity) planElement, null);
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
     * through the carpooling rider mode configured as teleporting mode)
     * so that PersonPrepareForSim does not reroute our legs (and thereby discards
     * our leg attributes where we store if the person found a match or not)
     */
    private void addRoutingModeAndRouteForRiders() {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            for (int i = 0; i < planElements.size(); i++) {
                if (planElements.get(i) instanceof Leg) {
                    Leg leg = (Leg) planElements.get(i);
                    if (!leg.getMode().equals(Carpooling.RIDER_MODE)) {
                        continue;
                    }
                    TripStructureUtils.setRoutingMode(leg, Carpooling.RIDER_MODE);

                    Activity prevActivity = (Activity) planElements.get(i - 1);
                    Activity nextActivity = (Activity) planElements.get(i + 1);
                    Leg calculatedLeg = CarpoolingUtil.calculateLeg(riderRouter,
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
