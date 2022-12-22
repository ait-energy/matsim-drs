package at.ac.ait.matsim.dominoridesharing.optimizer;

import at.ac.ait.matsim.dominoridesharing.request.CarpoolingRequest;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.DistanceUtils;

import java.util.ArrayList;

public class RequestsFilter {

    private final double driverMaxWaitTime;

    private final double driverMaxTravelTimeToPassenger;

    private final double passengerMaxWaitTime;

    private final double maxDistance;

    public RequestsFilter(double driverMaxWaitTime, double driverMaxTravelTimeToPassenger, double passengerMaxWaitTime, double maxDistance) {
        this.driverMaxWaitTime = driverMaxWaitTime;
        this.driverMaxTravelTimeToPassenger = driverMaxTravelTimeToPassenger;
        this.passengerMaxWaitTime = passengerMaxWaitTime;
        this.maxDistance = maxDistance;
    }

    public ArrayList<CarpoolingRequest> filterRequests(ArrayList<CarpoolingRequest> passengersRequests, Node driverOrigin, Node driverDestination, double driverDepartureTime){
        ArrayList<CarpoolingRequest> filteredPassengerRequests = new ArrayList<>();
        for (CarpoolingRequest passengersRequest : passengersRequests) {
            Node passengerOrigin = passengersRequest.getFromLink().getFromNode();
            Node passengerDestination = passengersRequest.getToLink().getFromNode();
            double passengerDepartureTime = passengersRequest.getSubmissionTime();
            double distanceAtOrigin = DistanceUtils.calculateDistance(driverOrigin, passengerOrigin);
            double distanceAtDestination = DistanceUtils.calculateDistance(driverDestination, passengerDestination);
            boolean condition1 = passengerDepartureTime - driverMaxWaitTime- driverMaxTravelTimeToPassenger < driverDepartureTime;
            boolean condition2 = driverDepartureTime < passengerDepartureTime + passengerMaxWaitTime;
            //Use TravelTimeMatrix or leave it like this or remove it completely
            boolean condition3 = distanceAtOrigin < maxDistance;
            boolean condition4 = distanceAtDestination < maxDistance;
            if (condition1 && condition2 && condition3 && condition4) {
                filteredPassengerRequests.add(passengersRequest);
            }
        }
        return filteredPassengerRequests;
    }
}
