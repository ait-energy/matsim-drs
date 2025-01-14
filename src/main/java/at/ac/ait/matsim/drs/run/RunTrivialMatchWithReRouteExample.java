package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Example demonstrating that the ReRoute strategy does not crash and does not
 * lead to bad scores for drsRider plans (This is more of a test for our example
 * config than the drs logic itself)
 */
public class RunTrivialMatchWithReRouteExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunTrivialMatchWithReRouteExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(10);
        config.controller().setOutputDirectory("output-floridsdorf-trivialMatchWithReRoute");
        config.plans().setInputFile("population_drs_trivialMatch.xml");

        // Configure replanning so that both ReRoute and SubtourModeChoice are used
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("BestScore").setWeight(0.1));
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("SubtourModeChoice").setWeight(0.4));
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.5));

        config.subtourModeChoice().setModes(new String[] { Drs.DRIVER_MODE, Drs.RIDER_MODE, TransportMode.bike });
        config.subtourModeChoice().setChainBasedModes(new String[] { Drs.DRIVER_MODE, TransportMode.bike });
    }

}
