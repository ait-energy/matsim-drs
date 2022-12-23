package at.ac.ait.matsim.domino.carpooling;

import org.junit.jupiter.api.Test;

import at.ac.ait.matsim.domino.carpooling.run.LoggingExampleMain;

import org.junit.jupiter.api.Assertions;

public class ExampleTest {

    @Test
    public void testEchomethod() {
        int data = 10;
        Assertions.assertEquals(data, LoggingExampleMain.echoMethod(data));
        // Assertions.fail("it's a fail!");
    }

}
