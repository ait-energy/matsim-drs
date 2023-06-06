package at.ac.ait.matsim.drs.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class RunSimpleDrsExample {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling.xml", new DrsConfigGroup());

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Drs.DRIVER_MODE);

        // necessary to kick-start the carpooling driver pool
        int count = DrsUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig());
        LOGGER.info("added initial carpooling driver plan to {} agent(s)", count);

        Controler controller = new Controler(scenario);

        // necessary to register the carpooling module
        Drs.prepareController(controller);

        controller.run();
    }
}