package at.ac.ait.matsim.domino.carpooling.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingExampleMain {

    private static Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        LOGGER.warn("this is a warning!");
        LOGGER.debug("we are going to call echomethod now..");
        int data = 10;
        int echoResult = echoMethod(data);

        // note the curly brace syntax:
        LOGGER.info("we passed {} to the echMethod and got {} in return.", data, echoResult);

    }

    public static int echoMethod(int data) {
        return data;
    }

}
