package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.analysis.StatsCollector;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.optimizer.*;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.router.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.facilities.FacilitiesUtils;

@SuppressWarnings("all")

public class PlansModifier implements StartupListener, ReplanningListener {
    Logger LOGGER = LogManager.getLogger();
    @Inject
    private TripRouter tripRouter;

    @Override
    public void notifyReplanning(ReplanningEvent event) {
        preplanDay(event);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        preplanDay(event);
    }

    private void preplanDay(ControlerEvent event) {
        Scenario eventScenario = event.getServices().getScenario();
        StatsCollector.createOutputDirectory(event.getServices().getIterationNumber());
        Population population = eventScenario.getPopulation();
        Network network = eventScenario.getNetwork();
        CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
        EventsManager eventsManager = new EventsManagerImpl();

        RoutingModule router = tripRouter.getRoutingModule(Carpooling.DRIVER_MODE);
        CarpoolingOptimizer optimizer = new CarpoolingOptimizer(network, cfgGroup, population, router,
                event.getServices().getIterationNumber());
        HashMap<CarpoolingRequest, CarpoolingRequest> matchMap = optimizer.optimize();

        PopulationFactory populationFactory = population.getFactory();
        LOGGER.info("Modifying carpooling agents plans started.");
        for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {
            modifyPlans(entry.getKey(), entry.getValue(), populationFactory, router);
        }
        LOGGER.info("Modifying carpooling agents plans finished.");
    }

    private void modifyPlans(CarpoolingRequest driverRequest, CarpoolingRequest riderRequest, PopulationFactory factory,
            RoutingModule router) {
        RoutingRequest toCustomer = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(driverRequest.getFromLink()),
                FacilitiesUtils.wrapLink(riderRequest.getFromLink()), driverRequest.getDepartureTime(),
                driverRequest.getPerson());
        List<? extends PlanElement> legToCustomerList = router.calcRoute(toCustomer);
        Leg legToCustomer = (Leg) legToCustomerList.get(0);
        double pickupTime = driverRequest.getDepartureTime() + legToCustomer.getTravelTime().seconds();

        addNewActivitiesToDriverPlan(driverRequest, riderRequest, factory, router, legToCustomerList);
        adjustRiderDepartureTime(riderRequest, pickupTime);
    }

    void addNewActivitiesToDriverPlan(CarpoolingRequest driverRequest, CarpoolingRequest riderRequest,
            PopulationFactory factory, RoutingModule router, List<? extends PlanElement> legToCustomerList) {
        Leg legToCustomer = CarpoolingUtil.getFirstLeg(legToCustomerList);
        CarpoolingUtil.setRoutingMode(legToCustomerList);

        double pickupTime = driverRequest.getDepartureTime() + legToCustomer.getTravelTime().seconds();
        Activity pickup = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getFromLink().getId());
        pickup.setEndTime(pickupTime);
        CarpoolingUtil.setActivityType(pickup, Carpooling.ActivityType.pickup);
        CarpoolingUtil.setRiderId(pickup, riderRequest.getPerson().getId());

        RoutingRequest withCustomer = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(riderRequest.getFromLink()),
                FacilitiesUtils.wrapLink(riderRequest.getToLink()), pickupTime, driverRequest.getPerson());
        List<? extends PlanElement> legWithCustomerList = router.calcRoute(withCustomer);
        Leg legWithCustomer = CarpoolingUtil.getFirstLeg(legWithCustomerList);
        CarpoolingUtil.setRoutingMode(legWithCustomerList);

        double dropoffTime = pickupTime + legWithCustomer.getTravelTime().seconds();
        Activity dropoff = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        CarpoolingUtil.setActivityType(dropoff, Carpooling.ActivityType.dropoff);
        CarpoolingUtil.setRiderId(dropoff, riderRequest.getPerson().getId());

        RoutingRequest afterCustomer = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLink(riderRequest.getToLink()), FacilitiesUtils.wrapLink(driverRequest.getToLink()),
                dropoffTime, driverRequest.getPerson());
        List<? extends PlanElement> legAfterCustomerList = router.calcRoute(afterCustomer);
        CarpoolingUtil.setRoutingMode(legAfterCustomerList);

        ArrayList<PlanElement> newRoute = new ArrayList<>();
        newRoute.addAll(legToCustomerList);
        newRoute.add(pickup);
        newRoute.addAll(legWithCustomerList);
        newRoute.add(dropoff);
        newRoute.addAll(legAfterCustomerList);
        Activity startActivity = driverRequest.getTrip().getOriginActivity();
        Activity endActivity = driverRequest.getTrip().getDestinationActivity();
        List<PlanElement> planElements = driverRequest.getPerson().getSelectedPlan().getPlanElements();
        LOGGER.warn("Before matching " + driverRequest.getPerson().getId().toString() + " had "
                + driverRequest.getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
        TripRouter.insertTrip(planElements, startActivity, newRoute, endActivity);
        LOGGER.warn("After matching " + driverRequest.getPerson().getId().toString() + " had "
                + driverRequest.getPerson().getSelectedPlan().getPlanElements().size() + " plan elements.");
    }

    void adjustRiderDepartureTime(CarpoolingRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    if (!(CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement) == null)) {
                        if (CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement) == riderRequest
                                .getId().toString()) {
                            CarpoolingUtil.setActivityOriginalDepartureTime((Activity) planElement,
                                    riderRequest.getDepartureTime());
                            LOGGER.warn("Before matching " + riderRequest.getPerson().getId().toString()
                                    + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                            ((Activity) planElement).setEndTime(pickupTime);
                            CarpoolingUtil.removeLinkageActivityToRiderRequest((Activity) planElement);
                            LOGGER.warn("After matching " + riderRequest.getPerson().getId().toString()
                                    + "'s departure is at " + ((Activity) planElement).getEndTime().seconds());
                            break;
                        }
                    }
                }
            }
        }
    }
}
