package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

/**
 * Example using a population with predefined drs modes
 */
public class RunSimpleDrsWithPredefinedDrsPlans extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunSimpleDrsWithPredefinedDrsPlans().run(false);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setOutputDirectory("output-floridsdorf-drs-predefinedPlans");
        config.plans().setInputFile("population_drs.xml");
    }

}
