package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UndoPlansTest {
    static Population population;
    PopulationFactory factory = population.getFactory();

    @BeforeAll
    static void setup() {
        population = PopulationUtils.readPopulation("data/vienna/testPopulation.xml");

    }

    @Test
    void testUndoCarpoolingDriversPlans() {
        for (Person person : population.getPersons().values()) {
            int planSizeOriginal = person.getSelectedPlan().getPlanElements().size();

            addPseudoPlanElements(person.getSelectedPlan().getPlanElements());
            int planSizeAfterModifying = person.getSelectedPlan().getPlanElements().size();

            UndoPlans.undoDriverPlan(person);
            int planSizeAfterUndoing = person.getSelectedPlan().getPlanElements().size();

            if (person.getId().toString().equals("10254-2_2_2#1")) {
                assertEquals(planSizeAfterUndoing, planSizeOriginal);
                assertEquals(planSizeAfterUndoing, planSizeAfterModifying - 8);
            }

        }
    }

    private void addPseudoPlanElements(List<PlanElement> planElements) {
        ArrayList<PlanElement> legToCustomerList = new ArrayList<>();
        Leg legToCustomer = factory.createLeg(Carpooling.DRIVER_MODE);
        legToCustomerList.add(legToCustomer);

        ArrayList<PlanElement> legWithCustomerList = new ArrayList<>();
        Leg legWithCustomer = factory.createLeg(Carpooling.DRIVER_MODE);
        legWithCustomerList.add(legWithCustomer);

        ArrayList<PlanElement> legAfterCustomerList = new ArrayList<>();
        Leg legAfterCustomer = factory.createLeg(Carpooling.DRIVER_MODE);
        legAfterCustomerList.add(legAfterCustomer);

        Activity pickup = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION, null);
        Activity dropoff = factory.createActivityFromLinkId(Carpooling.DRIVER_INTERACTION, null);
        ArrayList<PlanElement> newRoute = new ArrayList<>();
        newRoute.addAll(legToCustomerList);
        newRoute.add(pickup);
        newRoute.addAll(legWithCustomerList);
        newRoute.add(dropoff);
        newRoute.addAll(legAfterCustomerList);

        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(planElements);
        for (TripStructureUtils.Trip trip : trips) {
            Activity startActivity = trip.getOriginActivity();
            Activity endActivity = trip.getDestinationActivity();
            List<Leg> legs = trip.getLegsOnly();
            for (Leg leg : legs) {
                String mode = leg.getMode();
                if (mode.equals(Carpooling.DRIVER_MODE)) {
                    TripRouter.insertTrip(planElements, startActivity, newRoute, endActivity);
                }
            }
        }
    }
}