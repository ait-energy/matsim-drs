package at.ac.ait.matsim.drs.optimizer;

import java.util.List;

public record MatchingResult(List<DrsMatch> matches, List<DrsRequest> unmatchedDriverRequests,
                List<DrsRequest> unmatchedRiderRequests) {
};