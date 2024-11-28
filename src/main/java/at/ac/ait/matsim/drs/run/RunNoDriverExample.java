package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Minimal example demonstrating how unmatched riders are handled
 */
public class RunNoDriverExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunNoDriverExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(1);
        config.controller().setOutputDirectory("output-floridsdorf-noDriver");
        config.plans().setInputFile("population_drs_noDriver.xml");

        // configure replanning so that in iteration 1 SubtourModeChoice will be used,
        // and drsRider mode is tried out
        // (so that we can demonstrate the unmatched rider problem)
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("SubtourModeChoiceForDrs").setWeight(1));

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setSubtourModeChoiceModes(new String[] { Drs.RIDER_MODE, TransportMode.bike });
        drs.setSubtourModeChoiceChainBasedModes(new String[] { TransportMode.bike });
    }

}
