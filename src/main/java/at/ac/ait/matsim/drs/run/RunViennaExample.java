package at.ac.ait.matsim.drs.run;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class RunViennaExample {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/vienna/config_drs.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        editPopulation(scenario.getPopulation());

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Drs.DRIVER_MODE);

        // necessary to kick-start the drs driver pool
        int count = DrsUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig());
        LOGGER.info("added initial drs driver plan to {} agent(s)", count);

        Controler controller = new Controler(scenario);

        // necessary to register the drs module
        Drs.prepareController(controller);

        controller.run();
    }

    public static void editPopulation(Population population) {
        Random random = new Random(0);
        for (Person person : population.getPersons().values()) {
            // for now get rid of subpops
            person.getAttributes().removeAttribute("subpopulation");

            // everybody can use DRS
            String drsAffinity = PersonUtils.getCarAvail(person).equals("always") ? "driverOrRider" : "riderOnly";
            person.getAttributes().putAttribute("drsAffinity", drsAffinity);

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (((Leg) planElement).getMode().equals("ride")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode(Drs.RIDER_MODE);
                        }
                    } else if (((Leg) planElement).getMode().equals("car")) {
                        if (random.nextDouble() < 0.1) {
                            ((Leg) planElement).setMode(Drs.DRIVER_MODE);
                        }
                    }
                }
            }
        }
    }
}
