package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

public class EditNetwork {
    public static void main(String[] args) {
        Network network = NetworkUtils.readNetwork("data/floridsdorf/network_carpooling.xml");
        for (Link link:network.getLinks().values()){
            if (link.getAllowedModes().contains(TransportMode.car)){
                link.setFreespeed(8.33);
            }
        }
        new NetworkWriter(network).write("data/floridsdorf/network_carpooling.xml");
    }
}
