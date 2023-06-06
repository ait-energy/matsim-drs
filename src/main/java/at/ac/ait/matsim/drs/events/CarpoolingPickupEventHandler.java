package at.ac.ait.matsim.drs.events;

import org.matsim.core.events.handler.EventHandler;

public interface CarpoolingPickupEventHandler extends EventHandler {
    void handleEvent(CarpoolingPickupEvent event);
}
