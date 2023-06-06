package at.ac.ait.matsim.drs.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RunSimpleDrsExampleTest {

    @Test
    @Tag("IntegrationTest")
    public void testRunWithoutExceptions() {
        RunSimpleDrsExample.main(new String[] {});
    }
}
