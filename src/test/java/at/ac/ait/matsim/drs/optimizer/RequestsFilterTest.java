package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.RoutingForTests;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

class RequestsFilterTest {
        static Network network;
        static RequestsFilter requestsFilter;
        static DrsRequest driverRequest, request2, request3, request4, request5, request6;
        List<DrsRequest> passengersRequests = new ArrayList<>();

        @BeforeAll
        static void setup() {
                RoutingForTests routingForTests = new RoutingForTests("data/floridsdorf/network.xml");
                network = routingForTests.getNetwork();
                RoutingModule driverRouter = routingForTests.getDriverRouter();

                DrsConfigGroup cfg = new DrsConfigGroup();
                cfg.setRiderDepartureTimeAdjustmentSeconds(180);
                requestsFilter = new RequestsFilter(cfg, driverRouter);
                driverRequest = new DrsRequest(Id.create(1, Request.class), null, null, 8 * 60 * 60,
                                null, network.getLinks().get(Id.createLinkId(1540)), null, null);
                request2 = new DrsRequest(Id.create(2, Request.class), null, null, 8 * 60 * 60, null,
                                network.getLinks().get(Id.createLinkId(1674)), null, null);
                request3 = new DrsRequest(Id.create(3, Request.class), null, null, 11 * 60 * 60, null,
                                network.getLinks().get(Id.createLinkId(1540)), null, null);
                request4 = new DrsRequest(Id.create(4, Request.class), null, null, 7 * 60 * 60, null,
                                network.getLinks().get(Id.createLinkId(1540)), null, null);
                request5 = new DrsRequest(Id.create(5, Request.class), null, null, 8 * 60 * 60, null,
                                network.getLinks().get(Id.createLinkId(1540)), null, null);
                request6 = new DrsRequest(Id.create(6, Request.class), null, null, 8 * 60 * 60, null,
                                network.getLinks().get(Id.createLinkId(1037)), null, null);
        }

        @BeforeEach
        public void beforeEach() {
                passengersRequests = new ArrayList<>();
        }

        @Test
        void testDriverArrivesTooLateToPassengerDueToDistance() {
                passengersRequests.add(request2);
                assertEquals(0, requestsFilter.filterRequests(driverRequest, passengersRequests).size());
        }

        @Test
        void testDriverArrivesTooLateToPassengerDueToTime() {
                passengersRequests.add(request3);
                assertEquals(0, requestsFilter.filterRequests(driverRequest, passengersRequests).size());
        }

        @Test
        void testDriverArrivesTooEarlyToPassengerDueToTime() {
                passengersRequests.add(request4);
                assertEquals(0, requestsFilter.filterRequests(driverRequest, passengersRequests).size());
        }

        @Disabled // FIXME this test fails now
        @Test
        void testDriverArrivesOnTime() {
                passengersRequests.add(request5);
                passengersRequests.add(request6);
                assertEquals(2, requestsFilter.filterRequests(driverRequest, passengersRequests).size());
        }

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
                Coord workCoordinate = new Coord(5000, 1000);
                Activity activity2A = factory.createActivityFromCoord("work", workCoordinate);
                Leg leg2 = factory.createLeg(TransportMode.car);
                Activity activity3A = factory.createActivityFromCoord("home", homeCoordinate);
                activity1A.setStartTime(8 * 60 * 60);
                activity1A.setEndTime(10 * 60 * 60);
                planA.addActivity(activity1A);
                planA.addLeg(leg);
                activity2A.setStartTime(12 * 60 * 60);
                activity2A.setEndTime(14 * 60 * 60);
                planA.addActivity(activity2A);
                planA.addLeg(leg2);
                activity3A.setStartTime(20 * 60 * 60);
                activity3A.setEndTime(22 * 60 * 60);
                planA.addActivity(activity3A);
                personA.addPlan(planA);
                TripStructureUtils.Trip trip = TripStructureUtils.getTrips(personA.getSelectedPlan().getPlanElements())
                                .get(1);

                DrsRequest riderRequest = new DrsRequest(Id.create(1, Request.class), personA, trip,
                                trip.getOriginActivity().getEndTime().seconds(),
                                null, null, null, null);
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 20 * 60, 25 * 60));
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 25 * 60, 25 * 60));
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 30 * 60, 25 * 60));
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 20 * 60, 25 * 60));
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 25 * 60, 25 * 60));
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 30 * 60, 25 * 60));

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
                Coord workCoordinate = new Coord(5000, 1000);
                Activity activity2A = factory.createActivityFromCoord("work", workCoordinate);
                Leg leg2 = factory.createLeg(TransportMode.car);
                Activity activity3A = factory.createActivityFromCoord("home", homeCoordinate);
                activity1A.setStartTime(7 * 60 * 60);
                activity1A.setEndTime(7 * 60 * 60 + 40 * 60);
                planA.addActivity(activity1A);
                planA.addLeg(leg);
                activity2A.setStartTime(8 * 60 * 60);
                activity2A.setEndTime(8 * 60 * 60);
                planA.addActivity(activity2A);
                planA.addLeg(leg2);
                activity3A.setStartTime(8 * 60 * 60 + 20 * 60);
                activity3A.setEndTime(8 * 60 * 60 + 40 * 60);
                planA.addActivity(activity3A);
                personA.addPlan(planA);
                TripStructureUtils.Trip trip = TripStructureUtils.getTrips(personA.getSelectedPlan().getPlanElements())
                                .get(1);

                DrsRequest riderRequest = new DrsRequest(Id.create(1, Request.class), personA, trip,
                                trip.getOriginActivity().getEndTime().seconds(),
                                null, null, null, null);
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 20 * 60, 25 * 60));
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 21 * 60, 25 * 60));
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() + 19 * 60, 25 * 60));
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 20 * 60, 25 * 60));
                assertFalse(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 21 * 60, 25 * 60));
                assertTrue(RequestsFilter.checkRiderTimeConstraints(riderRequest,
                                trip.getOriginActivity().getEndTime().seconds() - 19 * 60, 25 * 60));

        }
}