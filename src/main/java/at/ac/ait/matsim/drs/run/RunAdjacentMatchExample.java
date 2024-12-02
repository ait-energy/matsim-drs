package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Minimal example demonstrating a single match found in the replanning phase
 * where driver and rider have start destination not on the same link but close
 * enough for them to match
 */
public class RunAdjacentMatchExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunAdjacentMatchExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(1);
        config.controller().setOutputDirectory("output-floridsdorf-adjacentMatch");
        config.plans().setInputFile("population_drs_adjacentMatch.xml");

        // configure replanning so that in iteration 1 SubtourModeChoice will be used,
        // and drsRider + drsDriver mode is tried out
        // (so that we can demonstrate the match)
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("SubtourModeChoiceForDrs").setWeight(1));

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setSubtourModeChoiceModes(new String[] { Drs.DRIVER_MODE, Drs.RIDER_MODE, TransportMode.bike });
        drs.setSubtourModeChoiceChainBasedModes(new String[] { Drs.DRIVER_MODE, TransportMode.bike });
    }

}
