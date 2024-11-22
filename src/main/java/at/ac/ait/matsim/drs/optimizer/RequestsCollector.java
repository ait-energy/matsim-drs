package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.collect.ImmutableList;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Collects all DRS trips (requests) from the selected plans of the population.
 * These trips are expected to only consist of a single leg, since matched
 * requests from a previous iteration must be reset via PlanModificationUndoer.
 */
public class RequestsCollector {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DrsConfigGroup cfgGroup;
    private final Population population;
    private final Network network;
    private final RoutingModule driverRouter;
    private List<DrsRequest> driverRequests;
    private List<DrsRequest> riderRequests;

    public RequestsCollector(DrsConfigGroup cfgGroup, Population population, Network network,
            RoutingModule driverRouter) {
        this.cfgGroup = cfgGroup;
        this.population = population;
        this.network = network;
        this.driverRouter = driverRouter;
    }

    public void collectRequests() {
        driverRequests = new ArrayList<>();
        riderRequests = new ArrayList<>();
        long requestId = 0;

        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip : trips) {
                Set<String> tripModes = DrsUtil.getModes(trip);
                if (!tripModes.contains(Drs.DRIVER_MODE) && !tripModes.contains(Drs.RIDER_MODE)) {
                    continue;
                }
                if (trip.getLegsOnly().size() > 1) {
                    System.err.println("hmmm");
                }
                if (tripModes.size() > 1) {
                    // safeguard - should not happen
                    LOGGER.warn("Drs trip for person {} contains multiple modes ({}), ignoring trip.",
                            person.getId(),
                            tripModes);
                }

                Activity startActivity = trip.getOriginActivity();
                Link fromLink = network.getLinks().get(startActivity.getLinkId());
                Activity endActivity = trip.getDestinationActivity();
                Link toLink = network.getLinks().get(endActivity.getLinkId());
                String tripMode = tripModes.iterator().next();

                String msg = "Link {} ({}) not found in drs network for person {}.";
                if (fromLink == null) {
                    LOGGER.warn(msg, startActivity.getLinkId(), DrsUtil.toWktPoint(startActivity),
                            person.getId());
                    continue;
                } else if (toLink == null) {
                    LOGGER.warn(msg, endActivity.getLinkId(), DrsUtil.toWktPoint(endActivity),
                            person.getId());
                    continue;
                }

                if (fromLink == toLink) {
                    LOGGER.debug("Ignoring drs request with equal from/to link ({}) for person {}", fromLink.getId(),
                            person.getId());
                    continue;
                }

                long id = ++requestId;
                DrsRequest request = new DrsRequest(Id.create(id, Request.class),
                        person, trip, tripMode, fromLink, toLink);
                if (Double.isInfinite(request.getNetworkRouteDistance())) {
                    // for most driver trips the leg already has a route
                    // (due to prepareForMobsim and PlanModificationUndoer)
                    // but rider trips won't have a distance, so let's calculate a network route
                    Leg driverLeg = DrsUtil.calculateLeg(driverRouter,
                            request.getFromLink(),
                            request.getToLink(),
                            request.getDepartureTime(),
                            request.getPerson());
                    request.setLegWithNetworkRoute(driverLeg);
                }

                double distance = request.getNetworkRouteDistance();
                if (tripMode.equals(Drs.DRIVER_MODE)) {
                    if (cfgGroup.getMinDriverLegMeters() <= 0 || cfgGroup.getMinDriverLegMeters() <= distance) {
                        driverRequests.add(request);
                    } else {
                        LOGGER.debug("Ignoring {} request below min distance for person {}", tripMode, person.getId());
                    }
                } else if (tripMode.equals(Drs.RIDER_MODE)) {
                    if (cfgGroup.getMinRiderLegMeters() <= 0 || cfgGroup.getMinRiderLegMeters() <= distance) {
                        riderRequests.add(request);
                    } else {
                        LOGGER.debug("Ignoring {} request below min distance for person {}", tripMode, person.getId());
                    }
                }
            }
        }
        LOGGER.info("Collected {} driver and {} rider requests", driverRequests.size(), riderRequests.size());

    }

    public List<DrsRequest> getDriverRequests() {
        return ImmutableList.copyOf(driverRequests);
    }

    public List<DrsRequest> getRiderRequests() {
        return ImmutableList.copyOf(riderRequests);
    }
}
