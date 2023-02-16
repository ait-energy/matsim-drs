package at.ac.ait.matsim.domino.carpooling.planModifier;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling.ActivityType;
import at.ac.ait.matsim.domino.carpooling.optimizer.*;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
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

public class DriverPlanModifier implements StartupListener, ReplanningListener {
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

    void modifyPlans(ControlerEvent event) {
        LOGGER.info("modify carpooling plans");

        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();
        Network network = eventScenario.getNetwork();
        CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
        RoutingModule router = tripRouter.getRoutingModule(Carpooling.DRIVER_MODE);
        ZonalSystem zonalSystem = new SquareGridSystem(network.getNodes().values(),cfgGroup.cellSize);
        RequestZonalRegistry originZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,true);
        RequestZonalRegistry destinationZonalRegistry = RequestZonalRegistry.createRequestZonalRegistry(zonalSystem,false);
        RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(cfgGroup);

        undoPlansModification(population);

        RequestsCollector requestsCollector = new RequestsCollector(population,network);
        RequestsRegister requestsRegister = new RequestsRegister(originZonalRegistry,destinationZonalRegistry,timeSegmentRegistry);
        NearestRequestsFinder nearestRequestsFinder = new NearestRequestsFinder(cfgGroup,requestsRegister);
        RequestsFilter requestsFilter = new RequestsFilter(cfgGroup, router);
        BestRequestFinder bestRequestFinder = new BestRequestFinder(router,cfgGroup);


        CarpoolingOptimizer carpoolingOptimizer = new CarpoolingOptimizer(requestsCollector, requestsRegister,nearestRequestsFinder, requestsFilter, bestRequestFinder);
        HashMap<CarpoolingRequest, CarpoolingRequest> matchMap = carpoolingOptimizer.match();
        LOGGER.error(matchMap.size()+" matches happened.");
        PopulationFactory populationFactory = population.getFactory();

        for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {
            modifyPlan(entry.getKey(),entry.getValue(),populationFactory,router);
        }
    }

    private void modifyPlan(CarpoolingRequest driverRequest,CarpoolingRequest riderRequest,PopulationFactory factory,RoutingModule router ) {
        LOGGER.error(driverRequest.getPerson().getId()+" matched with "+riderRequest.getPerson().getId()+". Pick up at "+riderRequest.getFromLink().getId());
        List<PlanElement> planElements = driverRequest.getPerson().getSelectedPlan().getPlanElements();
        LOGGER.info(driverRequest.getPerson().getId()+" had "+planElements.size()+" plan elements before matching.");

        RoutingRequest toCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(driverRequest.getFromLink()),FacilitiesUtils.wrapLink(riderRequest.getFromLink()), driverRequest.getDepartureTime(), driverRequest.getPerson());
        List<? extends PlanElement> legToCustomerList = router.calcRoute(toCustomer);
        Leg legToCustomer= (Leg) legToCustomerList.get(0);
        for (PlanElement planElement : legToCustomerList) {
            if (planElement instanceof Leg){
                planElement.getAttributes().putAttribute("routingMode", Carpooling.DRIVER_MODE);
            }
        }
        double pickupTime = driverRequest.getDepartureTime()+ legToCustomer.getTravelTime().seconds();
        Activity pickup = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getFromLink().getId());
        pickup.setEndTime(pickupTime);
        CarpoolingUtil.setActivityType(pickup, ActivityType.pickup);
        CarpoolingUtil.setRiderId(pickup, riderRequest.getPerson().getId());

        RoutingRequest withCustomer = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(riderRequest.getFromLink()),FacilitiesUtils.wrapLink(riderRequest.getToLink()), pickupTime, driverRequest.getPerson());
        List<? extends PlanElement> legWithCustomerList = router.calcRoute(withCustomer);
        Leg legWithCustomer= (Leg) legWithCustomerList.get(0);
        for (PlanElement planElement : legWithCustomerList) {
            if (planElement instanceof Leg){
                TripStructureUtils.setRoutingMode(((Leg) planElement), Carpooling.DRIVER_MODE);
            }
        }

        double dropoffTime = pickupTime+ legWithCustomer.getTravelTime().seconds();
        Activity dropoff = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION,
                riderRequest.getToLink().getId());
        dropoff.setEndTime(dropoffTime);
        CarpoolingUtil.setActivityType(dropoff, ActivityType.dropoff);
        CarpoolingUtil.setRiderId(dropoff, riderRequest.getPerson().getId());

        RoutingRequest afterCustomer= DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLink(riderRequest.getToLink()),FacilitiesUtils.wrapLink(driverRequest.getToLink()), dropoffTime, driverRequest.getPerson());
        List<? extends PlanElement> legAfterCustomerList = router.calcRoute(afterCustomer);
        for (PlanElement planElement : legAfterCustomerList) {
            if (planElement instanceof Leg){
                TripStructureUtils.setRoutingMode(((Leg) planElement), Carpooling.DRIVER_MODE);
            }
        }

        ArrayList<PlanElement> newRoute = new ArrayList<>();
        newRoute.addAll(legToCustomerList);
        newRoute.add(pickup);
        newRoute.addAll(legWithCustomerList);
        newRoute.add(dropoff);
        newRoute.addAll(legAfterCustomerList);
        Activity startActivity = driverRequest.getTrip().getOriginActivity();
        Activity endActivity = driverRequest.getTrip().getDestinationActivity();
        TripRouter.insertTrip(planElements, startActivity, newRoute, endActivity);
        LOGGER.info("Now "+driverRequest.getPerson().getId()+" has "+planElements.size()+" plan elements after matching.");
    }



    void undoPlansModification(Population population) {
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip: trips){
                Activity startActivity = trip.getOriginActivity();
                Activity endActivity = trip.getDestinationActivity();
                List<Leg> legs = trip.getLegsOnly();
                for (Leg leg: legs) {
                    String mode = leg.getMode();
                    if (mode.equals(Carpooling.DRIVER_MODE)) {
                        ArrayList<PlanElement> oldRoute = new ArrayList<>();
                        oldRoute.add(leg);
                        TripRouter.insertTrip(person.getSelectedPlan(), startActivity, oldRoute, endActivity);
                    }
                }
            }
            LOGGER.info("Before this iteration, "+entry.getValue()+" will start with "+person.getSelectedPlan().getPlanElements().size()+" plan elements.");
        }
    }
}
