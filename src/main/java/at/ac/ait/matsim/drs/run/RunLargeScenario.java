package at.ac.ait.matsim.drs.run;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class RunLargeScenario {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(
                "/home/mstraub/projects/matsim-salabim/scenarios/matsim_model_upper_austria_2023.1/test_config_5km.xml");
        // "/home/mstraub/projects/matsim-salabim/scenarios/matsim_model_vienna_xl_2023.1/config_carpooling.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        enforceMaxPopulationSize(scenario.getPopulation(), 10_000);
        // addAffinity(scenario.getPopulation());

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Drs.DRIVER_MODE);

        // necessary to kick-start the carpooling driver pool
        int count = DrsUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig(),
                "subpop_cordon_agents");
        LOGGER.info("added initial carpooling driver plan to {} agent(s)", count);

        Controler controller = new Controler(scenario);
        // necessary to register the carpooling module
        Drs.prepareController(controller);

        controller.run();
    }

    /**
     * Removes persons from the scenario (keeps the first N persons)
     * 
     * @return the number of removed persons
     */
    public static int enforceMaxPopulationSize(Population population, int maxPopulationSize) {
        if (population.getPersons().size() <= maxPopulationSize)
            return 0;

        List<Id<Person>> keys = new ArrayList<>(population.getPersons().keySet());
        List<Id<Person>> deleteKeys = keys.subList(maxPopulationSize, keys.size());
        deleteKeys.forEach(k -> population.removePerson(k));
        return deleteKeys.size();
    }

    public static void addAffinity(Population population) {
        for (Person person : population.getPersons().values()) {
            person.getAttributes().putAttribute(Drs.ATTRIB_AFFINITY, Drs.AFFINITY_DRIVER_OR_RIDER);
        }
    }

}
