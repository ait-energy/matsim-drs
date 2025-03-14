package at.ac.ait.matsim.drs.optimizer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

/**
 * Heavily inspired by
 * org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry
 */

public class RequestTimeSegmentRegistry {
    private final Map<Integer, Map<Id<Request>, DrsRequest>> requestsInTimeSegments;
    private final DrsConfigGroup drsConfig;

    public RequestTimeSegmentRegistry(DrsConfigGroup drsConfig) {
        this.drsConfig = drsConfig;
        this.requestsInTimeSegments = new HashMap<>();
    }

    public void addRequest(DrsRequest request) {
        int timeSegment = getTimeSegment(request.getDepartureTime(), drsConfig.getTimeSegmentLengthSeconds());
        Map<Id<Request>, DrsRequest> requestsInTimeSegment = requestsInTimeSegments.get(timeSegment);
        if (requestsInTimeSegment != null) {
            if (requestsInTimeSegments.get(timeSegment).get(request.getId()) != null) {
                throw new IllegalStateException(request + " is already in the registry");
            } else {
                requestsInTimeSegment.put(request.getId(), request);
            }
        } else {
            requestsInTimeSegment = new HashMap<>();
            requestsInTimeSegment.put(request.getId(), request);
            requestsInTimeSegments.put(timeSegment, requestsInTimeSegment);
        }
    }

    public void removeRequest(DrsRequest request) {
        int timeSegment = getTimeSegment(request.getDepartureTime(), drsConfig.getTimeSegmentLengthSeconds());
        if (requestsInTimeSegments.get(timeSegment).remove(request.getId()) == null) {
            throw new IllegalStateException(request + " is not in the registry");
        }
    }

    public Stream<DrsRequest> findNearestRequests(double departureTime) {
        int timeSegment = getTimeSegment(departureTime, drsConfig.getTimeSegmentLengthSeconds());
        Stream<DrsRequest> requestsInPreviousSegment = Stream.empty();
        Stream<DrsRequest> requestsInCurrentSegment = Stream.empty();
        Stream<DrsRequest> requestsInNextSegment = Stream.empty();
        if (requestsInTimeSegments.get(timeSegment - 1) != null) {
            requestsInPreviousSegment = requestsInTimeSegments.get(timeSegment - 1).values().stream();
        }
        if (requestsInTimeSegments.get(timeSegment) != null) {
            requestsInCurrentSegment = requestsInTimeSegments.get(timeSegment).values().stream();
        }
        if (requestsInTimeSegments.get(timeSegment + 1) != null) {
            requestsInNextSegment = requestsInTimeSegments.get(timeSegment + 1).values().stream();
        }
        return Stream.concat(Stream.concat(requestsInPreviousSegment, requestsInNextSegment), requestsInCurrentSegment);
    }

    static int getTimeSegment(double departureTime, int segmentLength) {
        if (segmentLength <= 0) {
            throw new RuntimeException("Time Segment length should be bigger than zero");
        }
        int timeSegment = 0;
        for (int i = 0; i <= departureTime; i = i + segmentLength) {
            timeSegment++;
        }
        return timeSegment;
    }

    public Map<Integer, Map<Id<Request>, DrsRequest>> getRequestsInTimeSegments() {
        return requestsInTimeSegments;
    }
}