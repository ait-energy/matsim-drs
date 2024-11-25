package at.ac.ait.matsim.drs;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class RoutingForTests {

    private Network network;
    private RoutingModule driverRouter;

    public RoutingForTests(String networkXmlPath) {
        network = NetworkUtils.readNetwork(networkXmlPath);
        DrsUtil.addNewAllowedModeToCarLinks(network, Drs.DRIVER_MODE);
        LeastCostPathCalculator dijkstra = new SpeedyDijkstra(SpeedyGraphBuilder.build(network),
                new FreeSpeedTravelTime(),
                new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
        driverRouter = new NetworkRoutingModule(Drs.DRIVER_MODE, PopulationUtils.getFactory(),
                network, dijkstra);
    }

    public Network getNetwork() {
        return network;
    }

    public RoutingModule getDriverRouter() {
        return driverRouter;
    }

}
