package at.ac.ait.matsim.domino.carpooling.run;

import org.apache.commons.compress.utils.Sets;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

public class EditNetwork {
    public static void main(String[] args) {
        Network network = NetworkUtils.readNetwork("data/vienna/network.xml");
        for (Link link:network.getLinks().values()){
            if (link.getAllowedModes().contains(TransportMode.car)){
                link.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingDriver","carpoolingPassenger"));
            }
        }
        new NetworkWriter(network).write("data/vienna/network_carpooling.xml");
    }
}
