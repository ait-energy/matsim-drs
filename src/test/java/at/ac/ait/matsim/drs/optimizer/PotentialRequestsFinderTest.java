package at.ac.ait.matsim.drs.optimizer;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PotentialRequestsFinderTest {
        List<DrsRequest> originNearRequests = new ArrayList<>();
        List<DrsRequest> destinationNearRequests = new ArrayList<>();
        List<DrsRequest> temporalNearRequests = new ArrayList<>();
        DrsRequest request1 = new DrsRequest(Id.create(1, Request.class), null, null, 8 * 60 * 60, null,
                        null,
                        null, null);
        DrsRequest request2 = new DrsRequest(Id.create(2, Request.class), null, null, 8 * 60 * 60, null,
                        null,
                        null, null);
        DrsRequest request3 = new DrsRequest(Id.create(3, Request.class), null, null, 8 * 60 * 60, null,
                        null,
                        null, null);

        @Test
        void testEmptyStreamsIntersection() {
                assertEquals(0,
                                PotentialRequestsFinder.getIntersection(new DrsConfigGroup(),
                                                originNearRequests.stream(), destinationNearRequests.stream(),
                                                temporalNearRequests.stream())
                                                .size());
        }

        @Test
        void testNoIntersectionBetweenStreams() {
                originNearRequests.add(request1);
                destinationNearRequests.add(request2);
                temporalNearRequests.add(request3);
                assertEquals(0,
                                PotentialRequestsFinder.getIntersection(new DrsConfigGroup(),
                                                originNearRequests.stream(), destinationNearRequests.stream(),
                                                temporalNearRequests.stream())
                                                .size());
        }

        @Test
        void testIntersectionButNotInAllStreams() {
                originNearRequests.add(request1);
                destinationNearRequests.add(request1);
                destinationNearRequests.add(request2);
                temporalNearRequests.add(request3);
                assertEquals(0,
                                PotentialRequestsFinder.getIntersection(new DrsConfigGroup(),
                                                originNearRequests.stream(), destinationNearRequests.stream(),
                                                temporalNearRequests.stream())
                                                .size());
        }

        @Test
        void testIntersectionInAllStreams() {
                originNearRequests.add(request1);
                destinationNearRequests.add(request1);
                temporalNearRequests.add(request1);
                destinationNearRequests.add(request2);
                temporalNearRequests.add(request3);
                assertEquals(1,
                                PotentialRequestsFinder.getIntersection(new DrsConfigGroup(),
                                                originNearRequests.stream(), destinationNearRequests.stream(),
                                                temporalNearRequests.stream())
                                                .size());
        }
}