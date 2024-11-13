package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Minimal example demonstrating what happens when rider agents are late for
 * pickup
 */
public class RunRidersLateForPickupExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunRidersLateForPickupExample().run(false);
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
        // with 2 minutes pickup time the bike rider is picked up
        // while the pedestrian is late and will get stuck
        drs.setPickupWaitingSeconds(120);
    }

}
