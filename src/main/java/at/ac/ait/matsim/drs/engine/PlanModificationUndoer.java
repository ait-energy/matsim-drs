package at.ac.ait.matsim.drs.engine;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.FacilitiesUtils;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Resets drs trips:
 * - restores the original departure time for riders
 * - removes detailed legs for both riders and drivers
 *
 * Note, that this means that all but the currently selected plan
 * in the output_plans.xml are cleaned.
 */
public class PlanModificationUndoer implements IterationStartsListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private final RoutingModule drsDriverRouter;

    @Inject
    public PlanModificationUndoer(TripRouter tripRouter) {
        this.drsDriverRouter = tripRouter.getRoutingModule(Drs.DRIVER_MODE);
    }

    @Override
    public double priority() {
        return 10;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        if (iterationStartsEvent.getIteration() > 0) {
            undoPlans(iterationStartsEvent);
        }
    }

    private void undoPlans(IterationStartsEvent event) {
        LOGGER.info("Undoing drs plans at the beginning of the iteration (before replanning)");
        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();

        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            undoDriverPlan(person);
            undoRiderPlan(person);
        }
        LOGGER.info("Undoing drs plans finished");
    }

    static void undoRiderPlan(Person person) {
        for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                if (!(DrsUtil.getActivityOriginalDepartureTime((Activity) planElement) == null)) {
                    LOGGER.debug("Before undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
                    ((Activity) planElement)
                            .setEndTime(DrsUtil.getActivityOriginalDepartureTime((Activity) planElement));
                    DrsUtil.setActivityOriginalDepartureTime((Activity) planElement, null);
                    LOGGER.debug("After undoing, " + person.getId().toString() + "'s departure time is "
                            + ((Activity) planElement).getEndTime().seconds());
                }
            } else if (planElement instanceof Leg) {
                if (planElement.getAttributes().getAttribute(Drs.ATTRIB_ASSIGNED_DRIVER) != null) {
                    DrsUtil.setAssignedDriver((Leg) planElement, null);
                }
                if (planElement.getAttributes().getAttribute(Drs.ATTRIB_DRS_STATUS) != null) {
                    DrsUtil.setDrsStatus(((Leg) planElement), null);
                }
            }
        }
    }

    private void undoDriverPlan(Person person) {
        Plan selectedPlan = person.getSelectedPlan();
        int before = selectedPlan.getPlanElements().size();

        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(selectedPlan);
        for (TripStructureUtils.Trip trip : trips) {
            String tripMode = TripStructureUtils.identifyMainMode(trip.getTripElements());
            if (!tripMode.equals(Drs.DRIVER_MODE)) {
                continue;
            }

            Activity from = trip.getOriginActivity();
            Activity to = trip.getDestinationActivity();
            RoutingRequest routingRequest = DefaultRoutingRequest.withoutAttributes(
                    FacilitiesUtils.wrapActivity(from),
                    FacilitiesUtils.wrapActivity(to),
                    trip.getOriginActivity().getEndTime().orElse(0),
                    person);
            List<? extends PlanElement> route = drsDriverRouter.calcRoute(routingRequest);
            TripRouter.insertTrip(selectedPlan, from, route, to);
        }
        if (before != selectedPlan.getPlanElements().size()) {
            LOGGER.debug("Before undoing, " + person.getId().toString() + " had " + before + " plan elements.");
            LOGGER.debug("After undoing, " + person.getId().toString() + " had "
                    + selectedPlan.getPlanElements().size() + " plan elements.");
        }
    }

}