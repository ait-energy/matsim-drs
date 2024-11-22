package at.ac.ait.matsim.drs.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
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
        enforceMaxPopulationSize(scenario.getPopulation(), 1_000);
        clearSubpopulation(scenario.getPopulation());
        addDrsAffinity(scenario.getPopulation());

        // optional steps to prepare the scenario
        new CarLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Drs.DRIVER_MODE);

        // necessary to kick-start the drs driver pool
        int count = DrsUtil.addDrsDriverPlans(scenario.getPopulation(), scenario.getConfig());
        LOGGER.info("added initial drs driver plan to {} agent(s)", count);

        Controler controller = new Controler(scenario);

        // necessary to register the drs module
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

    public static void addDrsAffinity(Population population) {
        for (Person person : population.getPersons().values()) {
            String drsAffinity = PersonUtils.getCarAvail(person).equals("always")
                    ? Drs.AFFINITY_DRIVER_OR_RIDER
                    : Drs.AFFINITY_RIDER_ONLY;
            person.getAttributes().putAttribute(Drs.ATTRIB_AFFINITY, drsAffinity);
        }
    }

    public static void clearSubpopulation(Population population) {
        for (Person person : population.getPersons().values()) {
            person.getAttributes().removeAttribute("subpopulation");
        }
    }

}
