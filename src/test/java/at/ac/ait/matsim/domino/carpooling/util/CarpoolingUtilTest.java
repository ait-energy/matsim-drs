package at.ac.ait.matsim.domino.carpooling.util;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import static org.junit.jupiter.api.Assertions.*;

class CarpoolingUtilTest {
    @Test
    void checkWithinAllowedAdjustmentTest() {
        Config config = ConfigUtils.createConfig();
        Population population = PopulationUtils.createPopulation(config);
        PopulationFactory factory = population.getFactory();
        Person personA = factory.createPerson(Id.createPersonId(1));
        Plan planA = factory.createPlan();
        Coord homeCoordinate = new Coord(2000, 0);
        Activity activity1A = factory.createActivityFromCoord("home", homeCoordinate);
        Leg leg = factory.createLeg(TransportMode.car);
        Coord workCoordinate = new Coord(5000,1000);
        Activity activity2A = factory.createActivityFromCoord("work", workCoordinate);
        Leg leg2 = factory.createLeg(TransportMode.car);
        Activity activity3A = factory.createActivityFromCoord("home", homeCoordinate);
        activity1A.setStartTime(8*60*60);
        activity1A.setEndTime(10*60*60);
        planA.addActivity(activity1A);
        planA.addLeg(leg);
        activity2A.setStartTime(12*60*60);
        activity2A.setEndTime(14*60*60);
        planA.addActivity(activity2A);
        planA.addLeg(leg2);
        activity3A.setStartTime(20*60*60);
        activity3A.setEndTime(22*60*60);
        planA.addActivity(activity3A);
        personA.addPlan(planA);
        TripStructureUtils.Trip trip =  TripStructureUtils.getTrips(personA.getSelectedPlan().getPlanElements()).get(1);

        CarpoolingRequest riderRequest = new CarpoolingRequest(Id.create(1, Request.class), personA,trip, trip.getOriginActivity().getEndTime().seconds(),
                null, null, null, null);
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+20 * 60,25* 60 ));
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+25 * 60,25* 60 ));
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+30 * 60,25* 60 ));
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-20 * 60,25* 60 ));
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-25 * 60,25* 60 ));
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-30 * 60,25* 60 ));

    }
    @Test
    void overlappingActivitiesTest() {
        Config config = ConfigUtils.createConfig();
        Population population = PopulationUtils.createPopulation(config);
        PopulationFactory factory = population.getFactory();
        Person personA = factory.createPerson(Id.createPersonId(1));
        Plan planA = factory.createPlan();
        Coord homeCoordinate = new Coord(2000, 0);
        Activity activity1A = factory.createActivityFromCoord("home", homeCoordinate);
        Leg leg = factory.createLeg(TransportMode.car);
        Coord workCoordinate = new Coord(5000,1000);
        Activity activity2A = factory.createActivityFromCoord("work", workCoordinate);
        Leg leg2 = factory.createLeg(TransportMode.car);
        Activity activity3A = factory.createActivityFromCoord("home", homeCoordinate);
        activity1A.setStartTime(7*60*60);
        activity1A.setEndTime(7*60*60+40*60);
        planA.addActivity(activity1A);
        planA.addLeg(leg);
        activity2A.setStartTime(8*60*60);
        activity2A.setEndTime(8*60*60);
        planA.addActivity(activity2A);
        planA.addLeg(leg2);
        activity3A.setStartTime(8*60*60+20*60);
        activity3A.setEndTime(8*60*60+40*60);
        planA.addActivity(activity3A);
        personA.addPlan(planA);
        TripStructureUtils.Trip trip =  TripStructureUtils.getTrips(personA.getSelectedPlan().getPlanElements()).get(1);

        CarpoolingRequest riderRequest = new CarpoolingRequest(Id.create(1, Request.class), personA,trip, trip.getOriginActivity().getEndTime().seconds(),
                null, null, null, null);
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+20 * 60,25* 60 ));
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+21 * 60,25* 60 ));
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()+19 * 60,25* 60 ));
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-20 * 60,25* 60 ));
        assertFalse(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-21 * 60,25* 60 ));
        assertTrue(CarpoolingUtil.checkRiderTimeConstraints(riderRequest,trip.getOriginActivity().getEndTime().seconds()-19 * 60,25* 60 ));

    }
}