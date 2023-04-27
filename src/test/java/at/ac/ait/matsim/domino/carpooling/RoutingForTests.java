package at.ac.ait.matsim.domino.carpooling;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.speedy.SpeedyDijkstra;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RoutingForTests {

    private Network network;
    private RoutingModule driverRouter;

    public RoutingForTests(String networkXmlPath) {
        network = NetworkUtils.readNetwork(networkXmlPath);
        CarpoolingUtil.addNewAllowedModeToCarLinks(network, Carpooling.DRIVER_MODE);
        LeastCostPathCalculator dijkstra = new SpeedyDijkstra(new SpeedyGraph(network),
                new FreeSpeedTravelTime(),
                new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
        driverRouter = new NetworkRoutingModule(Carpooling.DRIVER_MODE, PopulationUtils.getFactory(),
                network, dijkstra);
    }

    public Network getNetwork() {
        return network;
    }

    public RoutingModule getDriverRouter() {
        return driverRouter;
    }

}
