package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

public class RunSimpleDrsWithPredefinedPlans2 extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunSimpleDrsWithPredefinedPlans2().run(false);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output-floridsdorf-drs-predefinedPlans2");
        config.plans().setInputFile("population_drs2.xml");

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setCellSize(200);
    }

}
