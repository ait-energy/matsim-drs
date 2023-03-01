package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.optimizer.*;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
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
        modifyPlans(event);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        modifyPlans(event);
    }

    private void modifyPlans(ControlerEvent event) {

        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();
        Network network = eventScenario.getNetwork();
        CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
        RoutingModule router = tripRouter.getRoutingModule(Carpooling.DRIVER_MODE);
        ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(),cfgGroup.cellSize);
        RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,true);
        RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,false);
        RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(cfgGroup);

        LOGGER.info("Matching process started!");

        RequestsCollector requestsCollector = new RequestsCollector(population,network);
        RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry,destinationZonalRegistry,timeSegmentRegistry);
        NearestRequestsFinder nearestRequestsFinder = new NearestRequestsFinder(cfgGroup,requestsRegister);
        RequestsFilter requestsFilter = new RequestsFilter(cfgGroup, router);
        BestRequestFinder bestRequestFinder = new BestRequestFinder(router,cfgGroup);


        CarpoolingOptimizer carpoolingOptimizer = new CarpoolingOptimizer(requestsCollector, requestsRegister,nearestRequestsFinder, requestsFilter, bestRequestFinder);
        HashMap<CarpoolingRequest, CarpoolingRequest> matchMap = carpoolingOptimizer.match();
        LOGGER.info("Matching process finished!");
        LOGGER.info(matchMap.size()+" matches happened.");
        PopulationFactory populationFactory = population.getFactory();

        LOGGER.info("Modifying carpooling agents plans started.");
        for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {
            modifyPlan(entry.getKey(),entry.getValue(),populationFactory,router);
        }
    }

    private void modifyPlan(CarpoolingRequest driverRequest,CarpoolingRequest riderRequest,PopulationFactory factory,RoutingModule router ) {
        RoutingRequest toCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverRequest.getFromLink()),FacilitiesUtils.wrapLink(riderRequest.getFromLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
        List<? extends PlanElement> legToCustomerList = router.calcRoute(toCustomer);
        Leg legToCustomer= (Leg) legToCustomerList.get(0);
        double pickupTime = driverRequest.getDepartureTime()+ legToCustomer.getTravelTime().seconds();
        LOGGER.warn("Before matching "+ driverRequest.getPerson().getId().toString()+" had "+ driverRequest.getPerson().getSelectedPlan().getPlanElements().size()+" plan elements.");
        addNewActivitiesToDriverPlan(driverRequest,riderRequest,factory,router,legToCustomerList);
        LOGGER.warn("After matching "+ driverRequest.getPerson().getId().toString()+" had "+ driverRequest.getPerson().getSelectedPlan().getPlanElements().size()+" plan elements.");

        adjustRiderDepartureTime(riderRequest, pickupTime);
    }

    void addNewActivitiesToDriverPlan(CarpoolingRequest driverRequest, CarpoolingRequest riderRequest, PopulationFactory factory, RoutingModule router, List<? extends PlanElement> legToCustomerList){
        Leg legToCustomer= CarpoolingUtil.getFirstLeg(legToCustomerList);
        CarpoolingUtil.setRoutingMode(legToCustomerList);

        double pickupTime = driverRequest.getDepartureTime()+ legToCustomer.getTravelTime().seconds();
        Activity pickup = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getFromLink().getId());
        pickup.setEndTime(pickupTime);
        CarpoolingUtil.setActivityType(pickup, Carpooling.ActivityType.pickup);
        CarpoolingUtil.setRiderId(pickup, riderRequest.getPerson().getId());

        RoutingRequest withCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(riderRequest.getFromLink()),FacilitiesUtils.wrapLink(riderRequest.getToLink()), pickupTime, driverRequest.getPerson());
        List<? extends PlanElement> legWithCustomerList = router.calcRoute(withCustomer);
        Leg legWithCustomer= CarpoolingUtil.getFirstLeg(legWithCustomerList);
        CarpoolingUtil.setRoutingMode(legWithCustomerList);


        double dropoffTime = pickupTime+ legWithCustomer.getTravelTime().seconds();
        Activity dropoff = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        CarpoolingUtil.setActivityType(dropoff, Carpooling.ActivityType.dropoff);
        CarpoolingUtil.setRiderId(dropoff, riderRequest.getPerson().getId());

        RoutingRequest afterCustomer= DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(riderRequest.getToLink()),FacilitiesUtils.wrapLink(driverRequest.getToLink()), dropoffTime, driverRequest.getPerson());
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
        TripRouter.insertTrip(planElements, startActivity, newRoute, endActivity);
    }

    void adjustRiderDepartureTime(CarpoolingRequest riderRequest, double pickupTime) {
        if (riderRequest.getDepartureTime() > pickupTime) {
            List<PlanElement> planElements = riderRequest.getPerson().getSelectedPlan().getPlanElements();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    if (!(CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement)==null)) {
                        if (CarpoolingUtil.getLinkageActivityToRiderRequest((Activity) planElement)==riderRequest.getId().toString()){
                            CarpoolingUtil.setActivityOriginalDepartureTime((Activity) planElement,riderRequest.getDepartureTime());
                            ((Activity) planElement).setEndTime(pickupTime);
                            CarpoolingUtil.removeLinkageActivityToRiderRequest((Activity) planElement);
                            break;
                        }
                    }
                }
            }
        }
    }
}
