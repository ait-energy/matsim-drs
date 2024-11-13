package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Minimal example demonstrating what happens when rider agents are late for
 * pickup
 */
public class RunRidersLateForPickupExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunRidersLateForPickupExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output-floridsdorf-ridersLateForPickup");
        config.plans().setInputFile("population_drs_ridersLateForPickup.xml");
        // disable replaning
        config.replanning().clearStrategySettings();
        config.replanning().addStrategySettings(new StrategySettings().setStrategyName("SelectExpBeta").setWeight(1));

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setCellSize(200);
        drs.setPickupWaitingSeconds(120);
    }

}