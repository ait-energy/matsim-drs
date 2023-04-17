package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.domino.carpooling.util.CarLinkAssigner;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RunFixedCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling_fixed.xml",
                new CarpoolingConfigGroup());

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        CarpoolingUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        CarpoolingUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Carpooling.DRIVER_MODE);

        // necessary to kick-start the carpooling driver pool
        CarpoolingUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig());

        Controler controller = new Controler(scenario);
        // necessary to register the carpooling module
        Carpooling.prepareController(controller);

        controller.run();
    }
}
