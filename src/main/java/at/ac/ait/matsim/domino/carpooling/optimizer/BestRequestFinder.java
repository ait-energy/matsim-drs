package at.ac.ait.matsim.domino.carpooling.optimizer;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;

import java.util.ArrayList;
import java.util.HashMap;

public class BestRequestFinder {

    private final LeastCostPathCalculator leastCostPathCalculator;

    private final double detourFactorWeight;

    private final double driverWaitingTimeWeight;

    private final double passengerWaitingTimeWeight;

    public BestRequestFinder(LeastCostPathCalculator leastCostPathCalculator, double detourFactorWeight,
            double driverWaitingTimeWeight, double passengerWaitingTimeWeight) {
        this.leastCostPathCalculator = leastCostPathCalculator;
        this.detourFactorWeight = detourFactorWeight;
        this.driverWaitingTimeWeight = driverWaitingTimeWeight;
        this.passengerWaitingTimeWeight = passengerWaitingTimeWeight;
    }

    public CarpoolingRequest findBestRequest(CarpoolingRequest driverRequest,
            ArrayList<CarpoolingRequest> filteredPassengersRequests) {
        HashMap<CarpoolingRequest, Double> requestsScores = new HashMap<>();
        CarpoolingRequest bestPassengerRequest = null;
        Node driverOrigin = driverRequest.getFromLink().getFromNode();
        Node driverDestination = driverRequest.getFromLink().getFromNode();
        LeastCostPathCalculator.Path originalPath = leastCostPathCalculator.calcLeastCostPath(driverOrigin,
                driverDestination, 0, null, null);
        double originalPathTravelTime = originalPath.travelTime;
        for (CarpoolingRequest passengerRequest : filteredPassengersRequests) {
            Node passengerOrigin = passengerRequest.getFromLink().getFromNode();
            Node passengerDestination = passengerRequest.getFromLink().getFromNode();
            LeastCostPathCalculator.Path pathToCustomer = leastCostPathCalculator.calcLeastCostPath(driverOrigin,
                    passengerOrigin, 0, null, null);
            double t1 = pathToCustomer.travelTime;
            LeastCostPathCalculator.Path pathWithCustomer = leastCostPathCalculator.calcLeastCostPath(passengerOrigin,
                    passengerDestination, 0, null, null);
            double t2 = pathWithCustomer.travelTime;
            LeastCostPathCalculator.Path pathAfterCustomer = leastCostPathCalculator
                    .calcLeastCostPath(passengerDestination, driverDestination, 0, null, null);
            double t3 = pathAfterCustomer.travelTime;
            double newPathTravelTime = t1 + t2 + t3;
            double detourTime = originalPathTravelTime - newPathTravelTime; // Or detour factor?
            double driverWaitingTime = Math
                    .max(passengerRequest.getSubmissionTime() - (driverRequest.getSubmissionTime() + t1), 0);
            double passengerWaitingTime = Math
                    .max((driverRequest.getSubmissionTime() + t1) - passengerRequest.getSubmissionTime(), 0);

            double score = (-detourFactorWeight * detourTime) + (-driverWaitingTimeWeight * driverWaitingTime)
                    + (-passengerWaitingTimeWeight * passengerWaitingTime);

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
