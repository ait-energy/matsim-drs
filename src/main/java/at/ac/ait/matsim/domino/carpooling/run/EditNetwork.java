package at.ac.ait.matsim.domino.carpooling.run;

import org.apache.commons.compress.utils.Sets;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.Map;

public class EditNetwork {
    public static void main(String[] args) { Config config = ConfigUtils.loadConfig("data/floridsdorf/config_vanilla.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
        for (Map.Entry<Id<Link>, ? extends Link> entry : networkLinks.entrySet()) {
            if(entry.getValue().getAllowedModes().contains(TransportMode.car)){
                entry.getValue().setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingDriver","carpoolingPassenger"));
            }
        }
        new NetworkWriter(network).write("data/floridsdorf/network_carpooling.xml");
    }

}
