package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import java.util.ArrayList;
import java.util.HashMap;

public class BestRequestFinder {

    private final LeastCostPathCalculator router;
    private final Network network;
    private final CarpoolingConfigGroup cfgGroup;

    public BestRequestFinder(LeastCostPathCalculator router, Network network, CarpoolingConfigGroup cfgGroup) {
        this.router = router;
        this.network = network;
        this.cfgGroup = cfgGroup;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest,
            ArrayList<CarpoolingRequest> filteredPassengersRequests) {
        HashMap<CarpoolingRequest, Double> requestsScores = new HashMap<>();
        CarpoolingRequest bestPassengerRequest = null;
        Coord driverOrigin = driverRequest.getOrigin();
        Coord driverDestination = driverRequest.getDestination();
        Node driverOriginNode= NetworkUtils.getNearestNode(network,driverOrigin);
        Node driverDestinationNode = NetworkUtils.getNearestNode(network,driverDestination);
        LeastCostPathCalculator.Path originalPath = router.calcLeastCostPath(driverOriginNode,
                driverDestinationNode, 0, null, null);
        double originalPathTravelTime = originalPath.travelTime;
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests) {
            Coord passengerOrigin = passengerRequest.getOrigin();
            Coord passengerDestination = passengerRequest.getDestination();
            Node passengerOriginNode = NetworkUtils.getNearestNode(network,passengerOrigin);
            Node passengerDestinationNode = NetworkUtils.getNearestNode(network,passengerDestination);
            LeastCostPathCalculator.Path pathToCustomer = router.calcLeastCostPath(driverOriginNode,
                    passengerOriginNode, 0, null, null);
            double t1 = pathToCustomer.travelTime;
            LeastCostPathCalculator.Path pathWithCustomer = router.calcLeastCostPath(passengerOriginNode,
                    passengerDestinationNode, 0, null, null);
            double t2 = pathWithCustomer.travelTime;
            LeastCostPathCalculator.Path pathAfterCustomer = router
                    .calcLeastCostPath(passengerDestinationNode, driverDestinationNode, 0, null, null);
            double t3 = pathAfterCustomer.travelTime;
            double newPathTravelTime = t1 + t2 + t3;
            double detourTime = originalPathTravelTime - newPathTravelTime; // Or detour factor?
            double driverWaitingTime = Math
                    .max(passengerRequest.getSubmissionTime() - (driverRequest.getSubmissionTime() + t1), 0);
            double passengerWaitingTime = Math
                    .max((driverRequest.getSubmissionTime() + t1) - passengerRequest.getSubmissionTime(), 0);

            double score = (-cfgGroup.detourFactorWeight * detourTime) + (-cfgGroup.driverWaitingTimeWeight * driverWaitingTime)
                    + (-cfgGroup.passengerWaitingTimeWeight * passengerWaitingTime);

            requestsScores.put(passengerRequest, score);
        }

        double highestScore = Double.NEGATIVE_INFINITY;
        for (HashMap.Entry<CarpoolingRequest, Double> currentRequest : requestsScores.entrySet()) {
            if (currentRequest.getValue() > highestScore) {
                highestScore = currentRequest.getValue();
            }
        }
        for (HashMap.Entry<CarpoolingRequest, Double> currentRequest : requestsScores.entrySet()) {
            if (currentRequest.getValue() == highestScore) {
                bestPassengerRequest = currentRequest.getKey();
            }
        }
        return bestPassengerRequest;
    }
}
