package at.ac.ait.matsim.dominoridesharing;

import org.apache.log4j.Logger;

public class ExampleMain {

    private static Logger LOGGER = Logger.getLogger(ExampleMain.class);

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
