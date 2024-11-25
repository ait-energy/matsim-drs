package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Minimal example demonstrating a single match found in the replanning phase
 */
public class RunTrivialMatchExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunTrivialMatchExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(1);
        config.controller().setOutputDirectory("output-floridsdorf-trivialMatch");
        config.plans().setInputFile("population_drs_trivialMatch.xml");

        // force replanning to always try drs
        // (so that we can demonstrate the unmachted rider problem)
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("SubtourModeChoiceForDrs").setWeight(1));

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setSubtourModeChoiceModes(new String[] { Drs.DRIVER_MODE, Drs.RIDER_MODE, TransportMode.bike });
        drs.setSubtourModeChoiceChainBasedModes(new String[] { Drs.DRIVER_MODE, TransportMode.bike });
    }

}
