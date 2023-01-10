package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.util.DistanceUtils;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import java.util.ArrayList;

public class RequestsFilter {

    private final CarpoolingConfigGroup cfgGroup;

    public RequestsFilter(CarpoolingConfigGroup cfgGroup) {
        this.cfgGroup = cfgGroup;
    }

    public ArrayList<CarpoolingRequest> filterRequests(CarpoolingRequest driverRequest, ArrayList<CarpoolingRequest> passengersRequests) {
        ArrayList<CarpoolingRequest> filteredPassengerRequests = new ArrayList<>();
        Coord driverOrigin = driverRequest.getOrigin();
        Coord driverDestination = driverRequest.getDestination();
        double driverDepartureTime = driverRequest.getSubmissionTime();
        for (CarpoolingRequest passengersRequest : passengersRequests) {
            Coord passengerOrigin = passengersRequest.getOrigin();
            Coord passengerDestination = passengersRequest.getDestination();
            double passengerDepartureTime = passengersRequest.getSubmissionTime();
            double distanceAtOrigin = DistanceUtils.calculateDistance(driverOrigin, passengerOrigin);
            double distanceAtDestination = DistanceUtils.calculateDistance(driverDestination, passengerDestination);
            boolean driverDepartureTimeWindow = passengerDepartureTime - cfgGroup.driverMaxWaitTime - cfgGroup.driverMaxTravelTimeToPassenger < driverDepartureTime &&  driverDepartureTime < passengerDepartureTime + cfgGroup.passengerMaxWaitTime;
            boolean maxDistanceBetweenDriverAndPassengerAtOrigin = distanceAtOrigin < cfgGroup.maxDistance;
            boolean maxDistanceBetweenDriverAndPassengerAtDestination  = distanceAtDestination < cfgGroup.maxDistance;
            if (driverDepartureTimeWindow && maxDistanceBetweenDriverAndPassengerAtOrigin  && maxDistanceBetweenDriverAndPassengerAtDestination) {
                filteredPassengerRequests.add(passengersRequest);
            }
        }
        return filteredPassengerRequests;
    }
}
