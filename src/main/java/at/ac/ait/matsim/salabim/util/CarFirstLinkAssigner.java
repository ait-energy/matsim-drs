package at.ac.ait.matsim.salabim.util;

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
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;

/**
 * Tries to assign links to all activities of a population.
 * For multimodal networks it first tries to find a car link.
 * 
 * This should help with problems in MATSim v14 where a population initially
 * without links gets pt link assigned to facilities - which later causes
 * troubles with vehicles that actually have a proper route on the car network
 * but the previous facility has pt link assigned. In such cases you get the
 * following exception:
 * 
 * - DefaultTurnAcceptanceLogic:57 Cannot move vehicle ...
 */
public class CarFirstLinkAssigner extends AbstractPersonAlgorithm {
    private static Logger LOGGER = LogManager.getLogger();
    private Network network, carNetwork;
    private int radius = 500;

    public CarFirstLinkAssigner(Network network) {
        this.network = network;
        this.carNetwork = NetworkTools.createFilteredNetworkByLinkMode(network, ImmutableSet.of(TransportMode.car));
    }

    @Override
    public void run(Person person) {
        for (Plan plan : person.getPlans()) {
            for (PlanElement pe : plan.getPlanElements()) {
                if (pe instanceof Activity) {
                    Activity act = (Activity) pe;
                    if (act.getLinkId() == null) {
                        assignLink(person.getId(), act);
                    }
                }
            }
        }
    }

    private void assignLink(Id<Person> personId, Activity act) {
        Map<Double, Set<Link>> closestLinks = NetworkTools.findClosestLinks(carNetwork, act.getCoord(), radius, null);
        if (closestLinks.isEmpty()) {
            // no car link found, try the whole network
            closestLinks = NetworkTools.findClosestLinks(network, act.getCoord(), radius, null);
        }
        if (closestLinks.isEmpty()) {
            String wkt = String.format(Locale.US, "POINT(%.1f %.1f)", act.getCoord().getX(), act.getCoord().getY());
            LOGGER.warn("no link within {} m for activity at {} of agent {}", radius, wkt, personId.toString());
        } else {
            Link closest = closestLinks.values().iterator().next().iterator().next();
            act.setLinkId(closest.getId());
        }
    }
}