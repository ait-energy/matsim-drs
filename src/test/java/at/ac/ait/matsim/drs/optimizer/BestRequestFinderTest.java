package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.RoutingForTests;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;

class BestRequestFinderTest {
        static Network network;
        static DrsDriverRequest driverRequest;
        static DrsRiderRequest request2, request3, request4, request5;
        static List<? extends PlanElement> request2Route, request3Route, request4Route, request5Route;
        static RoutingRequest toRequest2, toRequest3, toRequest4, toRequest5;
        static BestRequestFinder bestRequestFinder;

        @BeforeAll
        static void setup() {
                RoutingForTests routingForTests = new RoutingForTests("data/floridsdorf/network.xml");
                network = routingForTests.getNetwork();
                RoutingModule driverRouter = routingForTests.getDriverRouter();

                bestRequestFinder = new BestRequestFinder(driverRouter);

                driverRequest = DrsTestUtil.mockDriverRequest(1, 8 * 60 * 60,
                                network.getLinks().get(Id.createLinkId(1540)),
                                network.getLinks().get(Id.createLinkId(186)));

                request2 = DrsTestUtil.mockRiderRequest(2, 8 * 60 * 60,
                                network.getLinks().get(Id.createLinkId(1541)),
                                network.getLinks().get(Id.createLinkId(186)));
                toRequest2 = DefaultRoutingRequest.withoutAttributes(
                                FacilitiesUtils.wrapLink(request2.getFromLink()),
                                FacilitiesUtils.wrapLink(request2.getToLink()),
                                driverRequest.getDepartureTime(), driverRequest.getPerson());
                request2Route = driverRouter.calcRoute(toRequest2);

                request3 = DrsTestUtil.mockRiderRequest(3, 8 * 60 * 60,
                                network.getLinks().get(Id.createLinkId(1037)),
                                network.getLinks().get(Id.createLinkId(186)));
                toRequest3 = DefaultRoutingRequest.withoutAttributes(
                                FacilitiesUtils.wrapLink(request3.getFromLink()),
                                FacilitiesUtils.wrapLink(request3.getToLink()),
                                driverRequest.getDepartureTime(), driverRequest.getPerson());
                request3Route = driverRouter.calcRoute(toRequest3);

                request4 = DrsTestUtil.mockRiderRequest(4, 8 * 60 * 60,
                                network.getLinks().get(Id.createLinkId(186)),
                                network.getLinks().get(Id.createLinkId(1037)));
                toRequest4 = DefaultRoutingRequest.withoutAttributes(
                                FacilitiesUtils.wrapLink(request4.getFromLink()),
                                FacilitiesUtils.wrapLink(request4.getToLink()),
                                driverRequest.getDepartureTime(), driverRequest.getPerson());
                request4Route = driverRouter.calcRoute(toRequest4);

                request5 = DrsTestUtil.mockRiderRequest(5, 8 * 60 * 60,
                                network.getLinks().get(Id.createLinkId(688)),
                                network.getLinks().get(Id.createLinkId(1540)));
                toRequest5 = DefaultRoutingRequest.withoutAttributes(
                                FacilitiesUtils.wrapLink(request5.getFromLink()),
                                FacilitiesUtils.wrapLink(request5.getToLink()),
                                driverRequest.getDepartureTime(), driverRequest.getPerson());
                request5Route = driverRouter.calcRoute(toRequest5);
        }

        @Test
        void noFilteredRequestsTest() {
                assertNull(bestRequestFinder.findBestRequest(Collections.emptyList()));
        }

        @Test
        void findBestRequestTest() {
                List<DrsMatch> potentialMatches = new ArrayList<>();
                potentialMatches.add(DrsMatch.createMinimal(driverRequest, request4, null));
                potentialMatches.add(DrsMatch.createMinimal(driverRequest, request5, null));
                potentialMatches.add(DrsMatch.createMinimal(driverRequest, request3, null));
                potentialMatches.add(DrsMatch.createMinimal(driverRequest, request2, null));

                DrsMatch bestMatch = bestRequestFinder.findBestRequest(potentialMatches);
                assertNotNull(bestMatch);
                assertEquals("2", bestMatch.getRider().getId().toString());
        }
}