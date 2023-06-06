package at.ac.ait.matsim.drs.optimizer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.RoutingModule;

import com.google.common.base.Functions;

import at.ac.ait.matsim.drs.request.CarpoolingMatch;
import at.ac.ait.matsim.drs.util.CarpoolingUtil;

public class BestRequestFinder {
    private final RoutingModule router;

    public BestRequestFinder(RoutingModule router) {
        this.router = router;
    }

    /**
     * @return null if no match was found
     */
    public CarpoolingMatch findBestRequest(List<CarpoolingMatch> matches) {
        if (matches.isEmpty()) {
            return null;
        }
        List<CarpoolingMatch> matchesWithDetails = matches.stream().map(m -> calculateDetailsForMatch(m))
                .collect(Collectors.toList());
        return findMatchWithLeastDetour(matchesWithDetails);
    }

    CarpoolingMatch calculateDetailsForMatch(CarpoolingMatch match) {
        // Should already be calculated (in the request collection phase)
        Leg originalDriverLeg = match.getDriver().getLegWithRoute();
        if (originalDriverLeg == null) {
            originalDriverLeg = CarpoolingUtil.calculateLeg(router,
                    match.getDriver().getFromLink(),
                    match.getDriver().getToLink(),
                    match.getDriver().getDepartureTime(),
                    match.getDriver().getPerson());
        }
        double originalRouteTravelTime = originalDriverLeg.getTravelTime().seconds();

        // should be calculated by RequestsFilter
        // if not then calculate it now.
        Leg toPickup = match.getAfterDropoff();
        if (toPickup == null) {
            toPickup = CarpoolingUtil.calculateLeg(router,
                    match.getDriver().getFromLink(),
                    match.getRider().getFromLink(),
                    match.getDriver().getDepartureTime(),
                    match.getDriver().getPerson());
        }
        double travelTimeToPickup = toPickup.getTravelTime().seconds();

        // should already be provided in the request,
        // if not calculate it now
        // NOTE: when using the cached leg the travelTimeToCustomer is not respected,
        // but that should be OK
        Leg withCustomer = match.getRider().getLegWithRoute();
        if (withCustomer == null) {
            withCustomer = CarpoolingUtil.calculateLeg(router,
                    match.getRider().getFromLink(),
                    match.getRider().getToLink(),
                    match.getDriver().getDepartureTime() + travelTimeToPickup,
                    match.getDriver().getPerson());
        }
        double travelTimeWithCustomer = withCustomer.getTravelTime().seconds();

        Leg afterDropoff = CarpoolingUtil.calculateLeg(router,
                match.getRider().getToLink(),
                match.getDriver().getToLink(),
                match.getDriver().getDepartureTime() + travelTimeToPickup + travelTimeWithCustomer,
                match.getDriver().getPerson());
        double travelTimeAfterDropoff = afterDropoff.getTravelTime().seconds();

        double newRouteTravelTime = travelTimeToPickup + travelTimeWithCustomer + travelTimeAfterDropoff;
        double detourFactor = newRouteTravelTime / originalRouteTravelTime;
        return CarpoolingMatch.create(match.getDriver(), match.getRider(), toPickup, withCustomer, afterDropoff,
                detourFactor);
    }

    static CarpoolingMatch findMatchWithLeastDetour(List<CarpoolingMatch> matches) {
        Map<CarpoolingMatch, Double> match2detour = matches.stream()
                .collect(Collectors.toMap(Functions.identity(), CarpoolingMatch::getDetourFactor));
        return Collections.min(match2detour.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
}