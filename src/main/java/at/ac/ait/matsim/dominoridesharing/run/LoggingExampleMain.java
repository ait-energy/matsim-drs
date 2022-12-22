package at.ac.ait.matsim.dominoridesharing.run;

import org.apache.log4j.Logger;

public class LoggingExampleMain {

    private static Logger LOGGER = Logger.getLogger(LoggingExampleMain.class);

    public static void main(String[] args) {
        LOGGER.warn("this is a warning!");
        LOGGER.debug("we are going to call echomethod now..");
        echoMethod(10);
    }

    public static int echoMethod(int data) {
        LOGGER.info("we got some data: " + data);
        return data;
    }

}
