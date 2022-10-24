package at.ac.ait.matsim.dominoridesharing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ExampleTest {

    @Test
    public void testEchomethod() {
        int data = 10;
        Assertions.assertEquals(data, ExampleMain.echoMethod(data));
        // Assertions.fail("it's a fail!");
    }

}
