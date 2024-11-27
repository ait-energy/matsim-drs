package at.ac.ait.matsim.drs.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.run.Drs;

class PlanModificationUndoerTest {
    static Population population;
    PopulationFactory factory = population.getFactory();

    @BeforeAll
    static void setup() {
        population = PopulationUtils.readPopulation("data/floridsdorf/population_drs.xml");
    }

    @Disabled // TODO reenable test with new API (requires a router)
    @Test
    void testUndoDrsDriversPlans() {
        for (Person person : population.getPersons().values()) {
            int planSizeOriginal = person.getSelectedPlan().getPlanElements().size();

            addPseudoPlanElements(person.getSelectedPlan().getPlanElements());
            int planSizeAfterModifying = person.getSelectedPlan().getPlanElements().size();

            // PlanModificationUndoer.undoDriverPlan(person);
            int planSizeAfterUndoing = person.getSelectedPlan().getPlanElements().size();

            if (person.getId().toString().equals("person2_drsDriver")) {
                assertEquals(planSizeAfterUndoing, planSizeOriginal);
                assertEquals(planSizeAfterUndoing, planSizeAfterModifying - 8);
            }

        }
    }

    private void addPseudoPlanElements(List<PlanElement> planElements) {
        ArrayList<PlanElement> legToCustomerList = new ArrayList<>();
        Leg legToCustomer = factory.createLeg(Drs.DRIVER_MODE);
        legToCustomerList.add(legToCustomer);

        ArrayList<PlanElement> legWithCustomerList = new ArrayList<>();
        Leg legWithCustomer = factory.createLeg(Drs.DRIVER_MODE);
        legWithCustomerList.add(legWithCustomer);

        ArrayList<PlanElement> legAfterCustomerList = new ArrayList<>();
        Leg legAfterCustomer = factory.createLeg(Drs.DRIVER_MODE);
        legAfterCustomerList.add(legAfterCustomer);

        Activity pickup = factory.createActivityFromLinkId(Drs.DRIVER_INTERACTION, null);
        Activity dropoff = factory.createActivityFromLinkId(Drs.DRIVER_INTERACTION, null);
        ArrayList<PlanElement> newRoute = new ArrayList<>(legToCustomerList);
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
                if (mode.equals(Drs.DRIVER_MODE)) {
                    TripRouter.insertTrip(planElements, startActivity, newRoute, endActivity);
                }
            }
        }
    }
}