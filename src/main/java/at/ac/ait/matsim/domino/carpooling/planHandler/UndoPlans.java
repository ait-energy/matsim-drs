package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UndoPlans implements IterationEndsListener {
    static Logger LOGGER = LogManager.getLogger();

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        undoPlans(event);
    }

    private void undoPlans(IterationEndsEvent event) {
        LOGGER.info("undoing carpooling plans started");
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
                    LOGGER.warn("Before undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
                    ((Activity) planElement)
                            .setEndTime(CarpoolingUtil.getActivityOriginalDepartureTime((Activity) planElement));
                    CarpoolingUtil.setActivityOriginalDepartureTime((Activity)planElement
                            ,CarpoolingUtil.getActivityOriginalDepartureTime((Activity) planElement));
                    LOGGER.warn("After undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
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
            LOGGER.warn("Before undoing, " + person.getId().toString() + " had " + before + " plan elements.");
            LOGGER.warn("After undoing, " + person.getId().toString() + " had "
                    + person.getSelectedPlan().getPlanElements().size() + " plan elements.");
        }
    }
}