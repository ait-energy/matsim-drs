package at.ac.ait.matsim.domino.carpooling.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RunSimpleCarpoolingExampleTest {

    /**
     * Simply check if the example runs without throwing an exception
     */
    @Test
    @Tag("IntegrationTest")
    public void testThatRoutingModeFromOutputPopulationIsRetained() {
        RunSimpleCarpoolingExample.main(new String[] {});
    }
}
