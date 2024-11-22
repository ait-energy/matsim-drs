package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

/**
 * Minimal example demonstrating what happens when rider agents are late for
 * pickup
 */
public class RunRidersDepartureTimeAdjustmentExample extends RunSimpleDrsExample {

    int adjustmentSeconds;

    public RunRidersDepartureTimeAdjustmentExample(int adjustmentSeconds) {
        this.adjustmentSeconds = adjustmentSeconds;
    }

    public static void main(String[] args) {
        new RunRidersDepartureTimeAdjustmentExample(15 * 60).run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output-floridsdorf-departureTimeAdjustment");
        config.plans().setInputFile("population_drs_departureTimeAdjustment.xml");

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setRiderDepartureTimeAdjustmentSeconds(adjustmentSeconds);
    }

}
