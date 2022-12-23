package at.ac.ait.matsim.domino.carpooling;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import at.ac.ait.matsim.domino.carpooling.run.RunOneTaxiExample;

public class RunOneTaxiExampleTest {

    @Test
    public void testRun() {
        Path config = Path.of("data", "one_taxi", "generic_dvrp_one_taxi_config.xml");
        // URL configUrl =
        // IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"),
        // "generic_dvrp_one_taxi_config.xml");
        RunOneTaxiExample.run(config, "one_taxi_vehicles.xml", false, 0);
    }
}
