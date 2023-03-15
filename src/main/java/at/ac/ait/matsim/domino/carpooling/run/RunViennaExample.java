package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

public class RunViennaExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/vienna/config_carpooling.xml");
        Carpooling.prepareConfig(config);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Carpooling.prepareScenario(scenario);

        editPopulation(scenario.getPopulation());

        Controler controller = new Controler(scenario);
        Carpooling.prepareController(controller);
        controller.run();
    }

    public static void editPopulation(Population population) {
        Random random = new Random(0);
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (((Leg) planElement).getMode().equals("ride")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode("carpoolingRider");
                        }
                    } else if (((Leg) planElement).getMode().equals("car")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode("carpoolingDriver");
                        }
                    }
                }
            }
        }
    }
}
