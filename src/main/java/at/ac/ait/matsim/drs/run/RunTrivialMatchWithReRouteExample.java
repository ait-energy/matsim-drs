package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

/**
 * Example demonstrating that the ReRoute strategy does not crash and does not
 * lead to bad scores for drsRider plans
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

        // configure replanning so ReRoute and SubtourModeChoice are used randomly
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("SubtourModeChoiceForDrs").setWeight(0.5));
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.5));

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setSubtourModeChoiceModes(new String[] { Drs.DRIVER_MODE, Drs.RIDER_MODE, TransportMode.bike });
        drs.setSubtourModeChoiceChainBasedModes(new String[] { Drs.DRIVER_MODE, TransportMode.bike });
    }

}
