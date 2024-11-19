package at.ac.ait.matsim.drs.run;

import org.matsim.core.config.Config;

/**
 * Main Example starting with regular plans (no drs modes yet).
 * DRS gets introduced via replanning.
 */
public class RunPerfectMatchExample extends RunSimpleDrsExample {

    public static void main(String[] args) {
        new RunPerfectMatchExample().run(true, null);
    }

    @Override
    public void adjustConfig(Config config) {
        config.controller().setLastIteration(100);
        config.controller().setOutputDirectory("output-floridsdorf-perfectMatch");
        config.plans().setInputFile("population_drs_perfectMatch.xml");

        DrsConfigGroup drs = (DrsConfigGroup) config.getModules().get("drs");
        drs.setCellSize(500);
    }

}