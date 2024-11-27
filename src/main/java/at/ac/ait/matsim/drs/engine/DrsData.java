package at.ac.ait.matsim.drs.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.pt2matsim.tools.NetworkTools;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

/**
 * Data that should only be prepared once (instead of each iteration)
 */
public class DrsData {

    private static final Logger LOGGER = LogManager.getLogger();

    private Network drsNetwork;
    private ZoneSystem zoneSystem;

    @Inject
    public DrsData(Scenario scenario, DrsConfigGroup drsConfig) {
        this.drsNetwork = NetworkTools.createFilteredNetworkByLinkMode(scenario.getNetwork(),
                ImmutableSet.of(Drs.DRIVER_MODE));
        LOGGER.info("Filtered {} drs driver links from network with {} links", drsNetwork.getLinks().size(),
                scenario.getNetwork().getLinks().size());

        this.zoneSystem = new SquareGridZoneSystem(scenario.getNetwork(), drsConfig.getCellSize());
        LOGGER.info("Initialized zone system.");
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public Network getDrsNetwork() {
        return drsNetwork;
    }

    public ZoneSystem getZoneSystem() {
        return zoneSystem;
    }
}
