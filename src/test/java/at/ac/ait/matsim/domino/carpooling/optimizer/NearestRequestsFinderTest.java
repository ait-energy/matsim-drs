package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NearestRequestsFinderTest {
    List<CarpoolingRequest> originNearRequests = new ArrayList<>();
    List<CarpoolingRequest> destinationNearRequests = new ArrayList<>();
    List<CarpoolingRequest> temporalNearRequests = new ArrayList<>();
    CarpoolingRequest request1 = new CarpoolingRequest(Id.create(1, Request.class), null, null, 8 * 60 * 60, null,
            null,
            null);
    CarpoolingRequest request2 = new CarpoolingRequest(Id.create(2, Request.class), null, null, 8 * 60 * 60, null,
            null,
            null);
    CarpoolingRequest request3 = new CarpoolingRequest(Id.create(3, Request.class), null, null, 8 * 60 * 60, null,
            null,
            null);

    @Test
    void testEmptyStreamsIntersection() {
        assertEquals(0,
                NearestRequestsFinder.getIntersection(new CarpoolingConfigGroup(),
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
                NearestRequestsFinder.getIntersection(new CarpoolingConfigGroup(),
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
                NearestRequestsFinder.getIntersection(new CarpoolingConfigGroup(),
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
                NearestRequestsFinder.getIntersection(new CarpoolingConfigGroup(),
                        originNearRequests.stream(), destinationNearRequests.stream(),
                        temporalNearRequests.stream())
                        .size());
    }
}