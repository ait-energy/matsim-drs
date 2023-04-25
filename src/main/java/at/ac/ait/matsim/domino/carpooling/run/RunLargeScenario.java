package at.ac.ait.matsim.domino.carpooling.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.domino.carpooling.util.CarLinkAssigner;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class RunLargeScenario {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(
                "/home/mstraub/projects/matsim-salabim/scenarios/matsim_model_upper_austria_2023.1/test_config.xml");
        // "/home/mstraub/projects/matsim-salabim/scenarios/matsim_model_vienna_xl_2023.1/config_carpooling.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // addAffinity(scenario.getPopulation());

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        CarpoolingUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        CarpoolingUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Carpooling.DRIVER_MODE);

        // necessary to kick-start the carpooling driver pool
        int count = CarpoolingUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig(),
                "subpop_cordon_agents");
        LOGGER.info("added initial carpooling driver plan to {} agent(s)", count);

        Controler controller = new Controler(scenario);
        // necessary to register the carpooling module
        Carpooling.prepareController(controller);

        controller.run();
    }

    public static void addAffinity(Population population) {
        for (Person person : population.getPersons().values()) {
            person.getAttributes().putAttribute(Carpooling.ATTRIB_AFFINITY, Carpooling.AFFINITY_DRIVER_OR_RIDER);
        }
    }

}