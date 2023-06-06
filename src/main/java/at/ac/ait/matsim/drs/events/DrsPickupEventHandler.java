package at.ac.ait.matsim.drs.events;

import org.matsim.core.events.handler.EventHandler;

public interface DrsPickupEventHandler extends EventHandler {
    void handleEvent(DrsPickupEvent event);
}
