package at.ac.ait.matsim.drs.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import at.ac.ait.matsim.drs.request.DrsRequest;
import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

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

        long riderRequestId = 0;
        long driverRequestId = 0;

        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            Person person = entry.getValue();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip : trips) {
                List<Leg> legs = trip.getLegsOnly();
                for (Leg leg : legs) {
                    String mode = leg.getMode();
                    if (!mode.equals(Drs.DRIVER_MODE) && !mode.equals(Drs.RIDER_MODE)) {
                        continue;
                    }

                    // FIXME what happens here? for each leg we don't take the leg start/end but
                    // trip start/end?? (ensure that trip must always consist of one leg only)
                    Activity startActivity = trip.getOriginActivity();
                    Link fromLink = network.getLinks().get(startActivity.getLinkId());
                    Activity endActivity = trip.getDestinationActivity();
                    Link toLink = network.getLinks().get(endActivity.getLinkId());
                    double activityEndTime = startActivity.getEndTime().seconds();

                    String msg = "link {} ({}) not found in carpooling network for person {}.";
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
                        continue;
                    }

                    long id = mode.equals(Drs.DRIVER_MODE) ? ++driverRequestId : ++riderRequestId;
                    DrsRequest request = new DrsRequest(Id.create(id, Request.class),
                            person, trip, activityEndTime, mode, fromLink, toLink, leg);
                    if (Double.isInfinite(request.getNetworkRouteDistance())) {
                        // for most driver trips the leg already has a route
                        // (due to prepareForMobsim and PlanModificationUndoer)
                        // but rider trips won't have a distance, so let's calculate a route
                        Leg driverLeg = DrsUtil.calculateLeg(driverRouter,
                                fromLink,
                                toLink,
                                activityEndTime,
                                person);
                        request.setLegWithRoute(driverLeg);
                    }

                    double distance = request.getNetworkRouteDistance();
                    if (mode.equals(Drs.DRIVER_MODE)) {
                        if (cfgGroup.getMinDriverLegMeters() <= 0 || cfgGroup.getMinDriverLegMeters() <= distance) {
                            driverRequests.add(request);
                        }
                    } else if (mode.equals(Drs.RIDER_MODE)) {
                        if (cfgGroup.getMinRiderLegMeters() <= 0 || cfgGroup.getMinRiderLegMeters() <= distance) {
                            riderRequests.add(request);
                        }
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
