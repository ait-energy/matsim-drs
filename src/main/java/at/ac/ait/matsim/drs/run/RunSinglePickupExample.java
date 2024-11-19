package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

/**
 * Demonstrating a single pickup that should happen without any delay (same
 * departure time, same start & end link)
 */
public class RunSinglePickupExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunSinglePickupExample().run(true, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output-floridsdorf-singlePickup");
        config.plans().setInputFile("population_drs_singlePickup.xml");

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setPickupWaitingSeconds(300);
    }

}