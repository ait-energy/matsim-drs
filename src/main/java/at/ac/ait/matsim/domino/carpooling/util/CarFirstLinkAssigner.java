package at.ac.ait.matsim.domino.carpooling.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;

/**
 * Tries to assign links to all activities of a population.
 * For multimodal networks it first tries to find a car link within the given
 * radius,
 * if none is available it searches for a different link within the radius.
 * If this does not succeed as well we just take the nearest car link (no matter
 * how far away).
 * <p>
 * This should help with problems in MATSim v14 where a population initially
 * without links gets pt link assigned to facilities - which later causes
 * troubles with vehicles that actually have a proper route on the car network
 * but the previous facility has pt link assigned. In such cases you get the
 * following exception:
 * - DefaultTurnAcceptanceLogic:57 Cannot move vehicle ...
 * <p>
 * Note that for populations with cordon agents (i.e. agents trying to start a
 * pt trip far outside the car network) this is important - if we simply take the
 * nearest car link the expected pt trip can not take place!
 */
public class CarFirstLinkAssigner extends AbstractPersonAlgorithm {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Network network;
    private final Network carNetwork;
    private final int radius;

    public CarFirstLinkAssigner(Network network, int radius) {
        this.network = network;
        this.radius = radius;
        this.carNetwork = NetworkTools.createFilteredNetworkByLinkMode(network, ImmutableSet.of(TransportMode.car));
    }

    @Override
    public void run(Person person) {
        for (Plan plan : person.getPlans()) {
            for (PlanElement pe : plan.getPlanElements()) {
                if (pe instanceof Activity) {
                    Activity act = (Activity) pe;
                    if (act.getLinkId() == null) {
                        // FIXME reliably assign pt links ONLY to pt cordon agents
                        act.setLinkId(NetworkUtils.getNearestLink(carNetwork, act.getCoord()).getId());
                        // assignLink(person.getId(), act);
                    }
                }
            }
        }
    }

    private void assignLink(Id<Person> personId, Activity act) {
        Map<Double, Set<Link>> closestLinks = NetworkTools.findClosestLinks(carNetwork, act.getCoord(), radius, null);
        if (closestLinks.isEmpty()) {
            closestLinks = NetworkTools.findClosestLinks(network, act.getCoord(), radius, null);
        }
        if (closestLinks.isEmpty()) {
            String wkt = String.format(Locale.US, "POINT(%.1f %.1f)", act.getCoord().getX(), act.getCoord().getY());
            LOGGER.info("no link within {} m for {} activity at {} of agent {}. Fallback to nearest car link.",
                    radius, act.getType(), wkt, personId.toString());
            act.setLinkId(NetworkUtils.getNearestLink(carNetwork, act.getCoord()).getId());
        } else {
            Link closest = closestLinks.values().iterator().next().iterator().next();
            act.setLinkId(closest.getId());
        }
    }
}