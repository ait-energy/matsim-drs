package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.*;

/*
 * We randomly shuffle the agents so each iteration, agents are not added in the same order.
 * This will guarantee that agents who are written first in the population file
 * will not always have a higher chance of matching.
 * Even after shuffling, agents who are first in the list will have a higher
 * chance but that can be ignored since in real life, passengers who come first
 * have probably a higher chance than the ones who come later.
 * However, it is still possible that a passenger who comes late in real life
 * gets a higher chance of matching in case by luck more drivers submitted
 * requests late as well.
 * Conclusion: shuffling agents order in the list before each iteration is
 * sufficient.
 */
public class RequestsCollector {
    private final ArrayList<CarpoolingRequest> driversRequests;
    private final ArrayList<CarpoolingRequest> passengersRequests;
    private final Population population;
    private long requestID = 0;

    public RequestsCollector(Population population,ArrayList<CarpoolingRequest> driversRequests, ArrayList<CarpoolingRequest> passengersRequests) {
        this.driversRequests = driversRequests;
        this.passengersRequests = passengersRequests;
        this.population = population;
    }

    public void collectRequests(){
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<Leg> legs = CarpoolingUtil.getLegs(person.getSelectedPlan());
            for (Leg leg: legs) {
                String mode = leg.getMode();
                if (mode.equals("carpoolingPassenger")){
                    List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
                    CarpoolingUtil.LegWithActivities activities = CarpoolingUtil.getActivitiesForLeg(planElements,leg);
                    Activity startActivity = activities.startActivity;
                    Activity endActivity = activities.endActivity;
                    double activityEndTime = startActivity.getEndTime().seconds();
                    Coord passengerOrigin = startActivity.getCoord();
                    Coord passengerDestination = endActivity.getCoord();
                    requestID = requestID+1;
                    CarpoolingRequest passengerRequest = new CarpoolingRequest(Id.create(requestID, Request.class), person, leg, activityEndTime,mode,passengerOrigin,passengerDestination);
                    passengersRequests.add(passengerRequest);
                }
                if (mode.equals("carpoolingDriver")){
                    List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
                    CarpoolingUtil.LegWithActivities activities = CarpoolingUtil.getActivitiesForLeg(planElements,leg);
                    Activity startActivity = activities.startActivity;
                    Activity endActivity = activities.endActivity;
                    double activityEndTime = startActivity.getEndTime().seconds();
                    Coord driverOrigin = startActivity.getCoord();
                    Coord driverDestination = endActivity.getCoord();
                    requestID = requestID+1;
                    CarpoolingRequest driverRequest = new CarpoolingRequest(Id.create(requestID, Request.class), person, leg, activityEndTime,mode,driverOrigin,driverDestination);
                    driversRequests.add(driverRequest);
                }
            }
        }
    }

    public ArrayList<CarpoolingRequest> getDriversRequests() {
        Collections.shuffle(driversRequests);
        return driversRequests;
    }

    public ArrayList<CarpoolingRequest> getPassengersRequests() {

        Collections.shuffle(passengersRequests);
        return passengersRequests;
    }
}
