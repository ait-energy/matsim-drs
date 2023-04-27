package at.ac.ait.matsim.domino.carpooling.planHandler;

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

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class PlanModificationUndoer implements IterationStartsListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private final RoutingModule carpoolingDriverRouter;

    @Inject
    public PlanModificationUndoer(TripRouter tripRouter) {
        this.carpoolingDriverRouter = tripRouter.getRoutingModule(Carpooling.DRIVER_MODE);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        undoPlans(iterationStartsEvent);
    }

    private void undoPlans(IterationStartsEvent event) {
        LOGGER.info("undoing carpooling plans at the beginning of the iteration before replan happens");
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
                if (planElement.getAttributes().getAttribute(Carpooling.ATTRIB_REQUEST_STATUS) != null) {
                    CarpoolingUtil.setRequestStatus((Leg) planElement, null);
                }
                if (planElement.getAttributes().getAttribute(Carpooling.ATTRIB_CARPOOLING_STATUS) != null) {
                    CarpoolingUtil.setCarpoolingStatus(((Leg) planElement), null);
                }
            }
        }
    }

    private void undoDriverPlan(Person person) {
        Plan selectedPlan = person.getSelectedPlan();
        int before = selectedPlan.getPlanElements().size();

        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(selectedPlan);
        for (TripStructureUtils.Trip trip : trips) {
            boolean isDriverTrip = false;
            for (Leg leg : trip.getLegsOnly()) {
                if (leg.getMode().equals(Carpooling.DRIVER_MODE)) {
                    isDriverTrip = true;
                }
            }
            if (!isDriverTrip) {
                continue;
            }

            // TODO performance improvement: skip rerouting if the trip consists of a single
            // leg with a route (maybe even further narrow it down to networkroutes?)

            Activity from = trip.getOriginActivity();
            Activity to = trip.getDestinationActivity();
            RoutingRequest routingRequest = DefaultRoutingRequest.withoutAttributes(
                    FacilitiesUtils.wrapActivity(from),
                    FacilitiesUtils.wrapActivity(to),
                    trip.getOriginActivity().getEndTime().orElse(0),
                    person);
            List<? extends PlanElement> route = carpoolingDriverRouter.calcRoute(routingRequest);
            TripRouter.insertTrip(selectedPlan, from, route, to);
        }
        if (before != selectedPlan.getPlanElements().size()) {
            LOGGER.debug("Before undoing, " + person.getId().toString() + " had " + before + " plan elements.");
            LOGGER.debug("After undoing, " + person.getId().toString() + " had "
                    + selectedPlan.getPlanElements().size() + " plan elements.");
        }
    }
}