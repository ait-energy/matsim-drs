package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.domino.carpooling.driver.CarpoolingDriverPlanModifier;

public class RunTransitExample {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_with_transit.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.addControlerListener(new CarpoolingDriverPlanModifier());
        controler.run();
    }
}
