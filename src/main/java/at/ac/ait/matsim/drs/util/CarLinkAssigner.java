package at.ac.ait.matsim.drs.util;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.pt2matsim.tools.NetworkTools;
// see PrepareForMobsimImpl for network filtering without pt2matsim

import com.google.common.collect.ImmutableSet;

/**
 * Assign car links to all activities of a population.
 */
public class CarLinkAssigner extends AbstractPersonAlgorithm {
    private final Network carNetwork;

    public CarLinkAssigner(Network network) {
        this.carNetwork = NetworkTools.createFilteredNetworkByLinkMode(network, ImmutableSet.of(TransportMode.car));
    }

    @Override
    public void run(Person person) {
        for (Plan plan : person.getPlans()) {
            for (PlanElement pe : plan.getPlanElements()) {
                if (pe instanceof Activity) {
                    Activity act = (Activity) pe;
                    if (act.getLinkId() == null) {
                        act.setLinkId(NetworkUtils.getNearestLink(carNetwork, act.getCoord()).getId());
                    }
                }
            }
        }
    }

}