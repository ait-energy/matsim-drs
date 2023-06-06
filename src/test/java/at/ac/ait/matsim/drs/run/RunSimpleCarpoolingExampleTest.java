package at.ac.ait.matsim.drs.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RunSimpleCarpoolingExampleTest {

    @Test
    @Tag("IntegrationTest")
    public void testRunWithoutExceptions() {
        RunSimpleCarpoolingExample.main(new String[] {});
    }
}
