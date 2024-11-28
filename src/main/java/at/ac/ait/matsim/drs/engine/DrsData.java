package at.ac.ait.matsim.drs.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.systems.grid.h3.H3Utils;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

/**
 * Data that should only be prepared once (instead of each iteration)
 */
public class DrsData {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int H3_MAX_LEVEL = 15;

    private Network drsNetwork;
    private H3ZoneSystem zoneSystem;

    @Inject
    public DrsData(Scenario scenario, DrsConfigGroup drsConfig) {
        this.drsNetwork = NetworkTools.createFilteredNetworkByLinkMode(scenario.getNetwork(),
                ImmutableSet.of(Drs.DRIVER_MODE));
        LOGGER.info("Filtered {} drs driver links from network with {} links", drsNetwork.getLinks().size(),
                scenario.getNetwork().getLinks().size());

        int resolution = findH3ResolutionForDistance(drsConfig.getMaxMatchingDistanceMeters());
        this.zoneSystem = new H3ZoneSystem(scenario.getConfig().global().getCoordinateSystem(), resolution,
                scenario.getNetwork(), Predicates.alwaysTrue());
        LOGGER.info("Initialized H3 zone system with resolution {}.", resolution);
    }

    public static int findH3ResolutionForDistance(int meters) {
        H3Core h3core = H3Utils.getInstance();
        for (int res = H3_MAX_LEVEL; res >= 0; res--) {
            double edgeLength = h3core.getHexagonEdgeLengthAvg(res, LengthUnit.m);
            if (edgeLength > meters) {
                return res;
            }
        }
        return 0;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public Network getDrsNetwork() {
        return drsNetwork;
    }

    public H3ZoneSystem getH3ZoneSystem() {
        return zoneSystem;
    }
}
