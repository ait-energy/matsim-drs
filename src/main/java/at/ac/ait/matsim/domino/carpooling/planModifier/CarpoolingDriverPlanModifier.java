package at.ac.ait.matsim.domino.carpooling.planModifier;

import at.ac.ait.matsim.domino.carpooling.optimizer.*;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.facilities.FacilitiesUtils;

public class CarpoolingDriverPlanModifier implements StartupListener, ReplanningListener {
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

        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();
        Network network = eventScenario.getNetwork();
        LeastCostPathCalculator router = new SpeedyDijkstra(new SpeedyGraph(network),new FreeSpeedTravelTime(),new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
        CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
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

        PopulationFactory populationFactory = population.getFactory();

       /* RouteFactories routeFactories = new RouteFactories();
        routeFactories.createRoute(NetworkRoute.class, passengerRequest.getToLink().getId(), driverRequest.getToLink().getId())
        legWithCustomer.setRoute(RouteUtils.createLinkNetworkRouteImpl(passengerRequest.getFromLink().getId(),passengerRequest.getToLink().getId()));*/
        for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {

            modifyPlan(entry.getKey(),entry.getValue(),populationFactory,eventScenario,router);
        }
    }

    private void modifyPlan(CarpoolingRequest driverRequest,CarpoolingRequest passengerRequest,PopulationFactory factory,Scenario scenario,LeastCostPathCalculator router ) {
        LOGGER.info(driverRequest.getPerson().getId()+" matched with "+passengerRequest.getPerson().getId()+".");
        List<PlanElement> planElements = driverRequest.getPerson().getSelectedPlan().getPlanElements();
        LOGGER.info(driverRequest.getPerson().getId()+" had "+planElements.size()+" plan elements before matching.");


        LeastCostPathCalculator.Path pathToCustomer = router.calcLeastCostPath(driverRequest.getFromLink().getFromNode(),
                passengerRequest.getFromLink().getFromNode(), 0, null, null);
        double pickupTime = driverRequest.getDepartureTime()+pathToCustomer.travelTime;
        Activity pickup = factory.createActivityFromLinkId("carpoolingDriver interaction",passengerRequest.getFromLink().getId());
        pickup.setEndTime(pickupTime);

        List<? extends PlanElement> legToCustomerList = tripRouter.calcRoute("carpoolingDriver",
                FacilitiesUtils.toFacility(driverRequest.getTrip().getOriginActivity(), scenario.getActivityFacilities()),
                FacilitiesUtils.toFacility(pickup, scenario.getActivityFacilities()),
                driverRequest.getTrip().getOriginActivity().getEndTime().orElse(0), driverRequest.getPerson(), null);

        LeastCostPathCalculator.Path pathWithCustomer = router.calcLeastCostPath(passengerRequest.getFromLink().getFromNode(),
                passengerRequest.getToLink().getFromNode(), 0, null, null);
        double dropoffTime = pickupTime+pathWithCustomer.travelTime;
        Activity dropoff = factory.createActivityFromLinkId("carpoolingDriver interaction",passengerRequest.getToLink().getId());
        dropoff.setEndTime(dropoffTime);

        List<? extends PlanElement> legWithCustomerList = tripRouter.calcRoute("carpoolingDriver",
                FacilitiesUtils.toFacility(pickup, scenario.getActivityFacilities()),
                FacilitiesUtils.toFacility(dropoff, scenario.getActivityFacilities()),
                pickup.getEndTime().orElse(0), driverRequest.getPerson(), null);

        List<? extends PlanElement> legAfterCustomerList = tripRouter.calcRoute("carpoolingDriver",
                FacilitiesUtils.toFacility(dropoff, scenario.getActivityFacilities()),
                FacilitiesUtils.toFacility(driverRequest.getTrip().getDestinationActivity(), scenario.getActivityFacilities()),
                dropoff.getEndTime().orElse(0), driverRequest.getPerson(), null);

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
                    if (mode.equals("carpoolingDriver")){
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
