package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

/**
 * Example using a population with predefined drs legs
 */
public class RunPredefinedDrsLegsExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunPredefinedDrsLegsExample().run(false, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output-floridsdorf-predefinedDrsLegs");
        config.plans().setInputFile("population_drs.xml");

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.maxMatchingDistanceMeters = 500;
    }

}
