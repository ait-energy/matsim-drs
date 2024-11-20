package at.ac.ait.matsim.drs.engine;

import org.matsim.core.events.handler.EventHandler;

public interface DrsPickupEventHandler extends EventHandler {
    void handleEvent(DrsPickupEvent event);
}
