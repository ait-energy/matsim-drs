package at.ac.ait.matsim.drs.run;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.CarpoolingUtil;

public class RunViennaExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/vienna/config_carpooling.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        editPopulation(scenario.getPopulation());

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

    public static void editPopulation(Population population) {
        Random random = new Random(0);
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (((Leg) planElement).getMode().equals("ride")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode(Carpooling.RIDER_MODE);
                        }
                    } else if (((Leg) planElement).getMode().equals("car")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode(Carpooling.DRIVER_MODE);
                        }
                    }
                }
            }
        }
    }
}
