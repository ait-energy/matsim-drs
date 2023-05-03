package at.ac.ait.matsim.domino.carpooling.events;

import org.matsim.core.events.handler.EventHandler;

public interface CarpoolingPickupEventHandler extends EventHandler {
    void handleEvent(CarpoolingPickupEvent event);
}
