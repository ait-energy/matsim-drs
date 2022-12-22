package at.ac.ait.matsim.dominoridesharing.optimizer;

import at.ac.ait.matsim.dominoridesharing.request.CarpoolingRequest;
import org.matsim.api.core.v01.network.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class CarpoolingOptimizer {
    private final BestRequestFinder bestRequestFinder;
    private final RequestsFilter requestsFilter;
    private final ArrayList<CarpoolingRequest> driversRequests;
    private final ArrayList<CarpoolingRequest> passengersRequests;


    /*TODO: Before adding the requests to the lists, try to find a way to randomly shuffle the agents so each iteration, agents are not added in the same order.
      This will guarantee that agents who are written first in the population file will not always have a higher chance of matching.
      Even after shuffling, agents who are first in the list will have a higher chance but that can be ignored since in real life, passengers who come first have probably a higher chance than the ones who come later.
      However, it is still possible that a passenger who comes late in real life gets a higher chance of matching in case by luck more drivers submitted requests late as well.
      Conclusion: shuffling agents order in the list before each iteration is sufficient.
      */

    public CarpoolingOptimizer(BestRequestFinder bestRequestFinder, RequestsFilter requestsFilter, ArrayList<CarpoolingRequest> driversRequests, ArrayList<CarpoolingRequest> passengersRequests) {
        this.bestRequestFinder = bestRequestFinder;
        this.requestsFilter = requestsFilter;
        this.driversRequests = driversRequests;
        this.passengersRequests = passengersRequests;
    }

    public HashMap<CarpoolingRequest, CarpoolingRequest> match(){
        HashMap<CarpoolingRequest, CarpoolingRequest> matchedRequests = new HashMap<>();
        for (int i = 0; i <driversRequests.size(); i++) {
                Node driverOrigin = driversRequests.get(i).getFromLink().getFromNode();
                Node driverDestination = driversRequests.get(i).getToLink().getFromNode();
                double driverDepartureTime = driversRequests.get(i).getSubmissionTime();
                ArrayList<CarpoolingRequest> filteredPassengersRequests = requestsFilter.filterRequests(passengersRequests,driverOrigin,driverDestination,driverDepartureTime);
                CarpoolingRequest bestPassengerRequest = bestRequestFinder.findBestRequest(driversRequests.get(i),filteredPassengersRequests);
                if(!(bestPassengerRequest ==null)){
                    matchedRequests.put(driversRequests.get(i),bestPassengerRequest);
                    driversRequests.remove(driversRequests.get(i));
                    passengersRequests.remove(bestPassengerRequest);
                }
        }
        return matchedRequests;
    }

}
