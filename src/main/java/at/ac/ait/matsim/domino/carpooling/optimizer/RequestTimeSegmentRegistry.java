package at.ac.ait.matsim.domino.carpooling.optimizer;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

public class RequestTimeSegmentRegistry {
    private final Map<Integer, ArrayList<CarpoolingRequest>> requestsInTimeSegments;
    private final SegmentLength segmentLength;
    private enum SegmentLength {

        HalfAnHour(48),
        OneHour(24),
        OneAndAHalfHour(16),
        TwoHours(12),
        ThreeHours(8);
        final int numberOfSegments;
        SegmentLength(int numberOfSegments) {
            this.numberOfSegments = numberOfSegments;
        }
    }

    public RequestTimeSegmentRegistry(Map<Integer, ArrayList<CarpoolingRequest>> requestsInTimeSegments, SegmentLength segmentLength) {
        this.requestsInTimeSegments = requestsInTimeSegments;
        this.segmentLength = segmentLength;
    }

    public void addRequest(CarpoolingRequest request) {
        int timeSegment = getTimeSegment(request);
        requestsInTimeSegments.get(timeSegment).add(request);
    }

    public void removeRequest(CarpoolingRequest request) {
        int timeSegment = getTimeSegment(request);
        requestsInTimeSegments.get(timeSegment).add(request);
    }

    public Stream<CarpoolingRequest> findClosestRequests(CarpoolingRequest request, int minCount) {
        return requestsInTimeSegments.get(getTimeSegment(request)).stream().limit(minCount);
    }


    private int getTimeSegment(CarpoolingRequest request) {
        int timeSegment = 0;
        int counter = 0;
        for (int i = 0; i < 24*60*60; i=i+(24*60*60/segmentLength.numberOfSegments)) {
            counter++;
            if (request.getSubmissionTime()>=i && request.getSubmissionTime()<(i+(24*60*60/segmentLength.numberOfSegments))){
                timeSegment = counter;
            }
        }
        return timeSegment;
        //Throw an exception when timeSegment is zero saying that the request is not submitted within the day
    }
}
