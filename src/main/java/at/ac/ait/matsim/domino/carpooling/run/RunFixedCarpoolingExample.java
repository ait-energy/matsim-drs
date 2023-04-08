package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.analysis.CarpoolingTripEventHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunFixedCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling_fixed.xml",
                new CarpoolingConfigGroup());

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Carpooling.prepareScenario(scenario);

        EventsManager manager = EventsUtils.createEventsManager();
        manager.addHandler(new CarpoolingTripEventHandler());
        new MatsimEventsReader(manager).readFile("output-floridsdorf-carpooling-fixed/output_events.xml.gz");

        Controler controller = new Controler(scenario);
        Carpooling.prepareController(controller);
        controller.run();
    }
}
