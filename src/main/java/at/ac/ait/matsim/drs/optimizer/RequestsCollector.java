package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.collect.ImmutableList;

import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsDriverRequest;
import at.ac.ait.matsim.drs.optimizer.DrsRequest.DrsRiderRequest;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Collects all DRS trips (requests) from the selected plans of the population.
 *
 * These trips are expected to be freshly routed / unmatched since
 * we reset results from the previous iteration in PlanModificationUndoer.
 * Trips should consist of one drs leg and optional access egress legs
 * (depending on routing.accessEgressType).
 */
public class RequestsCollector {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DrsConfigGroup drsConfig;
    private final Population population;
    private final Network drsNetwork;
    private final RoutingModule driverRouter;
    private List<DrsDriverRequest> driverRequests;
    private List<DrsRiderRequest> riderRequests;

    public RequestsCollector(DrsConfigGroup drsConfig, Population population, Network drsNetwork,
            RoutingModule driverRouter) {
        this.drsConfig = drsConfig;
        this.population = population;
        this.drsNetwork = drsNetwork;
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
                String tripMode = TripStructureUtils.identifyMainMode(trip.getTripElements());
                if (!DrsUtil.isDrsMode(tripMode)) {
                    continue;
                }

                Id<Request> id = Id.create(++requestId, Request.class);
                DrsRequest request = DrsRequest.create(id, driverRouter, drsNetwork, person, trip);

                double distance = request.getNetworkRouteDistance();
                if (request instanceof DrsDriverRequest) {
                    if (drsConfig.getMinDriverLegMeters() <= 0 || drsConfig.getMinDriverLegMeters() <= distance) {
                        driverRequests.add((DrsDriverRequest) request);
                    } else {
                        LOGGER.debug("Ignoring {} request below min distance for person {}", request.getMode(),
                                person.getId());
                    }
                } else if (request instanceof DrsRiderRequest) {
                    if (drsConfig.getMinRiderLegMeters() <= 0 || drsConfig.getMinRiderLegMeters() <= distance) {
                        riderRequests.add((DrsRiderRequest) request);
                    } else {
                        LOGGER.debug("Ignoring {} request below min distance for person {}", request.getMode(),
                                person.getId());
                    }
                }
            }
        }
    }

    public List<DrsDriverRequest> getDriverRequests() {
        return ImmutableList.copyOf(driverRequests);
    }

    public List<DrsRiderRequest> getRiderRequests() {
        return ImmutableList.copyOf(riderRequests);
    }
}
