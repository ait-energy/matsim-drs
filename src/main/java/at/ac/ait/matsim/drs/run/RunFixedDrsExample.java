package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class RunFixedDrsExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_drs_fixed.xml",
                new DrsConfigGroup());

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Drs.DRIVER_MODE);

        // necessary to kick-start the drs driver pool
        DrsUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig());

        Controler controller = new Controler(scenario);
        // necessary to register the drs module
        Drs.prepareController(controller);

        controller.run();
    }
}
