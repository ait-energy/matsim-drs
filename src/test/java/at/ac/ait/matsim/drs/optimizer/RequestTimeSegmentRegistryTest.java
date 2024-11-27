package at.ac.ait.matsim.drs.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import at.ac.ait.matsim.drs.DrsTestUtil;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

class RequestTimeSegmentRegistryTest {
    RequestTimeSegmentRegistry timeSegmentRegistry = new RequestTimeSegmentRegistry(
            new DrsConfigGroup());
    DrsRequest request1 = DrsTestUtil.mockRiderRequest(1, 8 * 60 * 60);
    DrsRequest request2 = DrsTestUtil.mockRiderRequest(2, 8 * 60 * 60);
    DrsRequest request3 = DrsTestUtil.mockRiderRequest(3, 11 * 60 * 60);

    @Test
    void testDepartureTimeEdges() {
        assertEquals(1, RequestTimeSegmentRegistry.getTimeSegment(0, 4 * 60 * 60));
        assertEquals(1, RequestTimeSegmentRegistry.getTimeSegment(4 * 60 * 60 - 1, 4 * 60 * 60));
        assertEquals(2, RequestTimeSegmentRegistry.getTimeSegment(4 * 60 * 60, 4 * 60 * 60));
        assertEquals(2, RequestTimeSegmentRegistry.getTimeSegment(5 * 60 * 60 + 1, 4 * 60 * 60));
        assertEquals(5, RequestTimeSegmentRegistry.getTimeSegment(8 * 60 * 60 + 1, 2 * 60 * 60));
    }

    @Test
    void testSegmentLengthZeroOrNegative() {
        assertThrows(RuntimeException.class, () -> {
            RequestTimeSegmentRegistry.getTimeSegment(21 * 60 * 60, 0);
        });
        assertThrows(RuntimeException.class, () -> {
            RequestTimeSegmentRegistry.getTimeSegment(21 * 60 * 60, -1);
        });
    }

    @Test
    void testAddRequestToANewSegment() {
        assertEquals(0, timeSegmentRegistry.getRequestsInTimeSegments().size());
        timeSegmentRegistry.addRequest(request1);
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().size());
        assertEquals(8 * 60 * 60,
                timeSegmentRegistry.getRequestsInTimeSegments().get(5).get(request1.getId()).getDepartureTime());
    }

    @Test
    void testAddSameRequestTwice() {
        timeSegmentRegistry.addRequest(request1);
        assertThrows(RuntimeException.class, () -> {
            timeSegmentRegistry.addRequest(request1);
        });
    }

    @Test
    void testAddMoreThanOneRequestInOneSegment() {
        assertEquals(0, timeSegmentRegistry.getRequestsInTimeSegments().size());
        timeSegmentRegistry.addRequest(request1);
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().size());
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
        timeSegmentRegistry.addRequest(request2);
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().size());
        assertEquals(2, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
    }

    @Test
    void testAddRequestsToDifferentSegments() {
        assertEquals(0, timeSegmentRegistry.getRequestsInTimeSegments().size());
        timeSegmentRegistry.addRequest(request1);
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().size());
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
        timeSegmentRegistry.addRequest(request3);
        assertEquals(2, timeSegmentRegistry.getRequestsInTimeSegments().size());
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().get(6).size());
    }

    @Test
    void testRemoveRequestInRegistry() {
        timeSegmentRegistry.addRequest(request1);
        timeSegmentRegistry.addRequest(request2);
        assertEquals(2, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
        timeSegmentRegistry.removeRequest(request1);
        assertEquals(1, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
        timeSegmentRegistry.removeRequest(request2);
        assertEquals(0, timeSegmentRegistry.getRequestsInTimeSegments().get(5).size());
    }

    @Test
    void testRemoveRequestNotInRegistry() {
        assertThrows(RuntimeException.class, () -> {
            timeSegmentRegistry.removeRequest(request1);
        });
    }

    @Test
    void testFindBestRequestWhenNoSegmentsAvailable() {
        assertEquals(0, timeSegmentRegistry.findNearestRequests(20 * 60 * 60).count());
        assertNotNull(timeSegmentRegistry.findNearestRequests(20 * 60 * 60).count());
    }

    @Test
    void testFindBestRequestWhenSegmentsAvailableButEmpty() {
        timeSegmentRegistry.addRequest(request1);
        timeSegmentRegistry.removeRequest(request1);
        assertEquals(0, timeSegmentRegistry.findNearestRequests(8 * 60 * 60).count());
        assertNotNull(timeSegmentRegistry.findNearestRequests(8 * 60 * 60).count());
    }

    @Test
    void testFindBestRequestWhenSegmentsAvailableAndNotEmpty() {
        timeSegmentRegistry.addRequest(request1);
        timeSegmentRegistry.addRequest(request2);
        timeSegmentRegistry.addRequest(request3);
        assertEquals(3, timeSegmentRegistry.findNearestRequests(8 * 60 * 60).count());
    }
}
