package at.ac.ait.matsim.drs.engine;

import at.ac.ait.matsim.drs.optimizer.DrsRequest;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanModifierTest {

    static DrsRequest request;
    static Activity activity1;
    static Activity activity2;

    @BeforeEach
    void setup() {
        Config config = ConfigUtils.createConfig();
        Population population = PopulationUtils.createPopulation(config);
        PopulationFactory factory = population.getFactory();
        Coord homeCoordinate = new Coord(0, 0);
        Coord workCoordinate = new Coord(0, 0);
        Person person = factory.createPerson(Id.createPersonId(1));
        Plan plan = factory.createPlan();
        activity1 = factory.createActivityFromCoord("home", homeCoordinate);
        activity1.setEndTime(8 * 60 * 60);
        plan.addActivity(activity1);
        Leg leg = factory.createLeg(Drs.RIDER_MODE);
        plan.addLeg(leg);
        activity2 = factory.createActivityFromCoord("work", workCoordinate);
        activity2.setEndTime(12 * 60 * 60);
        plan.addActivity(activity2);

        person.addPlan(plan);
        request = new DrsRequest(Id.create(1, Request.class), person, null, 8 * 60 * 60, null,
                null, null, null);
        DrsUtil.setLinkageActivityToRiderRequest(activity1, request.getId().toString());
    }

    @Test
    void testEarlyPickUpTime() {
        double earlyPickupTime = (8 * 60 * 60) - (0.25 * 60 * 60);
        PlanModifier.adjustRiderDepartureTime(request, earlyPickupTime);
        assertEquals(activity1.getEndTime().seconds(), earlyPickupTime);
    }

    @Test
    void testLatePickUpTime() {
        double latePickupTime = (8 * 60 * 60) + (0.25 * 60 * 60);
        PlanModifier.adjustRiderDepartureTime(request, latePickupTime);
        assertEquals(activity1.getEndTime().seconds(), request.getDepartureTime());
    }

    @Test
    void testExactPickUpTime() {
        double exactPickupTime = (8 * 60 * 60);
        PlanModifier.adjustRiderDepartureTime(request, exactPickupTime);
        assertEquals(activity1.getEndTime().seconds(), request.getDepartureTime());
    }

}