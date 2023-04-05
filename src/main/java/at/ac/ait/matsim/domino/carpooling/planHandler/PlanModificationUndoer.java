package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlanModificationUndoer implements IterationStartsListener {
    static Logger LOGGER = LogManager.getLogger();

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        undoPlans(iterationStartsEvent);
    }

    private void undoPlans(IterationStartsEvent event) {
        LOGGER.info("undoing carpooling plans at the beginning of the iteration before replanning happens");
        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();

        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            undoDriverPlan(person);
            undoRiderPlan(person);
        }
        LOGGER.info("undoing carpooling plans finished");
    }

    static void undoRiderPlan(Person person) {
        for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                if (!(CarpoolingUtil.getActivityOriginalDepartureTime((Activity) planElement) == null)) {
                    LOGGER.debug("Before undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
                    ((Activity) planElement)
                            .setEndTime(CarpoolingUtil.getActivityOriginalDepartureTime((Activity) planElement));
                    CarpoolingUtil.setActivityOriginalDepartureTime((Activity) planElement, null);
                    LOGGER.debug("After undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
                }
            } else if (planElement instanceof Leg) {
                if (planElement.getAttributes().getAttribute(Carpooling.ATTRIB_REQUEST_STATUS)!=null){
                    CarpoolingUtil.setDropoffStatus((Leg) planElement,null);
                } else if (planElement.getAttributes().getAttribute(Carpooling.ATTRIB_LEG_STATUS)!=null) {
                    CarpoolingUtil.setRequestStatus((Leg) planElement,null);
                }
                if (Objects.equals(((Leg) planElement).getMode(), Carpooling.MOBILITY_GUARANTEE_SHUTTLE)){
                    ((Leg) planElement).setMode(Carpooling.RIDER_MODE);
                }
            }
        }
    }

    static void undoDriverPlan(Person person) {
        int before = person.getSelectedPlan().getPlanElements().size();

        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
        for (TripStructureUtils.Trip trip : trips) {
            Activity startActivity = trip.getOriginActivity();
            Activity endActivity = trip.getDestinationActivity();
            List<Leg> legs = trip.getLegsOnly();
            for (Leg leg : legs) {
                String mode = leg.getMode();
                if (mode.equals(Carpooling.DRIVER_MODE)) {
                    ArrayList<PlanElement> oldRoute = new ArrayList<>();
                    oldRoute.add(leg);
                    TripRouter.insertTrip(person.getSelectedPlan(), startActivity, oldRoute, endActivity);
                }
            }
        }
        if (before != person.getSelectedPlan().getPlanElements().size()) {
            LOGGER.debug("Before undoing, " + person.getId().toString() + " had " + before + " plan elements.");
            LOGGER.debug("After undoing, " + person.getId().toString() + " had "
                    + person.getSelectedPlan().getPlanElements().size() + " plan elements.");
        }
    }
}