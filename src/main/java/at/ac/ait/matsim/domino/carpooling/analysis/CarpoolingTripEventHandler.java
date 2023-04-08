package at.ac.ait.matsim.domino.carpooling.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;

public class CarpoolingTripEventHandler implements PersonMoneyEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    public CarpoolingTripEventHandler() {
    }

    @Override
    public void handleEvent(PersonMoneyEvent moneyEvent) {
       LOGGER.warn(moneyEvent.getPurpose());
    }

    @Override
    public void reset(int iteration) {
        PersonMoneyEventHandler.super.reset(iteration);
    }
}