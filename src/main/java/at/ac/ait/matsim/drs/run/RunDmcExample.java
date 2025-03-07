package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

public class RunDmcExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunDmcExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(1);
        config.controller().setOutputDirectory("output-floridsdorf-dmc");
        config.plans().setInputFile("population_drs_longTour.xml");

        // configure replanning so that in iteration 1 DiscreteModeChoice will be used
        config.replanning().clearStrategySettings();
        config.replanning()
                .addStrategySettings(new StrategySettings().setStrategyName("DiscreteModeChoice").setWeight(1));
    }

}
