package at.ac.ait.matsim.drs.engine;

import org.matsim.core.events.handler.EventHandler;

public interface DrsFailedPickupEventHandler extends EventHandler {
    void handleEvent(DrsFailedPickupEvent event);
}
