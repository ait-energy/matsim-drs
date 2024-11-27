package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

class PotentialRequestsFinderTest {
        List<DrsRequest> originNearRequests = new ArrayList<>();
        List<DrsRequest> destinationNearRequests = new ArrayList<>();
        List<DrsRequest> temporalNearRequests = new ArrayList<>();
        DrsRequest request1 = DrsTestUtil.mockRiderRequest(1, 8 * 60 * 60);
        DrsRequest request2 = DrsTestUtil.mockRiderRequest(2, 8 * 60 * 60);
        DrsRequest request3 = DrsTestUtil.mockRiderRequest(3, 8 * 60 * 60);

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