package at.ac.ait.matsim.dominoridesharing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class Main {

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_vanilla.xml"); // args[0]);

        // possibly modify config here

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // possibly modify scenario here

        Controler controler = new Controler(scenario);

        // possibly modify controler here

        // uncomment the next line for a live visualization.
        // note, that OTFVis is deprecated and outdated (but maybe it's still useful)
        // controler.addOverridingModule(new OTFVisLiveModule());

        controler.run();
    }

}