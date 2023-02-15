package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.domino.carpooling.Carpooling;

public class RunSimpleCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling_simulatedPassengers.xml");
        Carpooling.prepareConfig(config);
        // config.network().setInputFile("network.xml");
        // config.plans().setInputFile("population_carpooling_solved.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Carpooling.prepareScenario(scenario);

        Controler controller = new Controler(scenario);
        Carpooling.prepareController(controller);
        controller.run();
    }
}