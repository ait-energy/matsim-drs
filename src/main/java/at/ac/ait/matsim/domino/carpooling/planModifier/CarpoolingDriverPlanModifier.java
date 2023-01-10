package at.ac.ait.matsim.domino.carpooling.planModifier;

import at.ac.ait.matsim.domino.carpooling.optimizer.BestRequestFinder;
import at.ac.ait.matsim.domino.carpooling.optimizer.CarpoolingOptimizer;
import at.ac.ait.matsim.domino.carpooling.optimizer.RequestsCollector;
import at.ac.ait.matsim.domino.carpooling.optimizer.RequestsFilter;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
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

public class CarpoolingDriverPlanModifier implements StartupListener, ReplanningListener {
    Logger LOGGER = LogManager.getLogger();

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

        restorePopulationPlans(population);

        CarpoolingConfigGroup cfgGroup = new CarpoolingConfigGroup("cfgGroup");
        ArrayList<CarpoolingRequest> driversRequests = new ArrayList<>();
        ArrayList<CarpoolingRequest> passengersRequests = new ArrayList<>();
        RequestsCollector requestsCollector = new RequestsCollector(population,driversRequests,passengersRequests);
        RequestsFilter requestsFilter = new RequestsFilter(cfgGroup);
        BestRequestFinder bestRequestFinder = new BestRequestFinder(router,network,cfgGroup);
        CarpoolingOptimizer carpoolingOptimizer = new CarpoolingOptimizer(requestsCollector,requestsFilter,bestRequestFinder);
        HashMap<CarpoolingRequest, CarpoolingRequest> matchMap = carpoolingOptimizer.match();

        PopulationFactory factory = population.getFactory();
        for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {
            LOGGER.info(entry.getKey().getPerson().getId()+" matched with "+entry.getValue().getPerson().getId()+".");
            Leg oldLeg = entry.getKey().getLeg();

            List<PlanElement> planElements = entry.getKey().getPerson().getSelectedPlan().getPlanElements();
            LOGGER.info(entry.getKey().getPerson().getId()+" had "+planElements.size()+" plan elements before matching.");
            int orderOfOldLeg = planElements.indexOf(oldLeg);

            Leg legToCustomer = factory.createLeg("carpoolingDriver");

            Coord pickupCoordinates = entry.getValue().getOrigin();
            Coord driverOrigin = entry.getKey().getOrigin();
            Node driverOriginNode= NetworkUtils.getNearestNode(network,driverOrigin);
            Node passengerOriginNode = NetworkUtils.getNearestNode(network,pickupCoordinates);
            LeastCostPathCalculator.Path pathToCustomer = router.calcLeastCostPath(driverOriginNode,
                    passengerOriginNode, 0, null, null);
            double pathToCustomerTravelTime = pathToCustomer.travelTime;
            CarpoolingUtil.LegWithActivities activities =CarpoolingUtil.getActivitiesForLeg(planElements,oldLeg);
            Activity startActivity = activities.startActivity;
            Activity pickup = factory.createActivityFromCoord("carpoolingInteraction", pickupCoordinates);
            pickup.setEndTime(startActivity.getEndTime().seconds()+pathToCustomerTravelTime);

            Leg legWithCustomer = factory.createLeg("carpoolingDriver");

            Coord dropoffCoordinates = entry.getValue().getDestination();
            Node passengerDestinationNode = NetworkUtils.getNearestNode(network,dropoffCoordinates);
            LeastCostPathCalculator.Path pathWithCustomer = router.calcLeastCostPath(passengerOriginNode,
                    passengerDestinationNode, 0, null, null);
            double pathWithCustomerTravelTime = pathWithCustomer.travelTime;
            Activity dropoff = factory.createActivityFromCoord("carpoolingInteraction", dropoffCoordinates);
            dropoff.setEndTime(startActivity.getEndTime().seconds()+pathToCustomerTravelTime+pathWithCustomerTravelTime);

            Leg legAfterCustomer = factory.createLeg("carpoolingDriver");

            planElements.add(orderOfOldLeg,legAfterCustomer);
            planElements.add(orderOfOldLeg,dropoff);
            planElements.add(orderOfOldLeg,legWithCustomer);
            planElements.add(orderOfOldLeg,pickup);
            planElements.add(orderOfOldLeg,legToCustomer);
            planElements.remove(oldLeg);
            LOGGER.info("Now "+entry.getKey().getPerson().getId()+" has "+planElements.size()+" plan elements after matching.");
        }
    }

    void restorePopulationPlans(Population population) {
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            List<PlanElement> planElements = entry.getValue().getSelectedPlan().getPlanElements();

            List<PlanElement> toBeDeleted = new ArrayList<>();
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    String type = ((Activity) planElement).getType();
                    if (type.equals("carpoolingInteraction")) {
                        toBeDeleted.add(planElement);
                    }
                }
            }
            planElements.removeAll(toBeDeleted);
            int legsNumber = 0;
            for (PlanElement planElement : planElements) {
                if (planElement instanceof Activity) {
                    legsNumber = 0;
                } else if (planElement instanceof Leg) {
                    legsNumber++;
                    if (legsNumber>1){
                        toBeDeleted.add(planElement);
                    }
                }
            }
            planElements.removeAll(toBeDeleted);
            LOGGER.info("Before this iteration, "+entry.getValue()+" will start with "+planElements.size()+" plan elements.");
        }
    }

}
