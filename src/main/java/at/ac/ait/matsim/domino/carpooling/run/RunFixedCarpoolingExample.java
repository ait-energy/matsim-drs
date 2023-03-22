package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunFixedCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling.xml", new CarpoolingConfigGroup());
        Carpooling.prepareConfig(config);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Carpooling.prepareScenario(scenario);

        Controler controller = new Controler(scenario);
        Carpooling.prepareController(controller);
        controller.run();
    }
}
