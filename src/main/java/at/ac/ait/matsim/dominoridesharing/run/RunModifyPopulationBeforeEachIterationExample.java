package at.ac.ait.matsim.dominoridesharing.run;

import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

import at.ac.ait.matsim.salabim.util.SalabimUtil;

/**
 * This example demonstrates how we can modify plans of a population before each
 * mobsim iteration.
 * 
 * Note: why didn't we use BeforeMobsimListener?
 * Because custom listeners are always called after core listeners,
 * which means that the PopulationWriter will be called before
 * we can change the legs - and that's not ideal.
 * 
 * So before iteration 0 our class gets called by StartupListener
 * and for all iterations after that by ReplanningListener.
 */
public class RunModifyPopulationBeforeEachIterationExample {

    private static Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_vanilla.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addControlerListener(new TrivialPopulationLegTwiddler());
        controler.addControlerListener(new FieldInjectedPopulationLegTwiddler());
        // controler.addControlerListener(new Factory().build());
        controler.run();
    }

    private static class FieldInjectedPopulationLegTwiddler implements StartupListener, ReplanningListener {

        @Inject
        private Scenario scenario;
        @Inject
        private Population population;
        @Inject
        private TripRouter tripRouter;

        @Override
        public void notifyReplanning(ReplanningEvent event) {
            twiddleLegs(event);
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            twiddleLegs(event);
        }

        void twiddleLegs(ControlerEvent event) {
            LOGGER.info("leg twiddler called.");

            Scenario eventScenario = event.getServices().getScenario();
            LOGGER.info("We are talking about the same scenario ? {}", this.scenario == eventScenario);
            LOGGER.info("We are talking about the same population ? {}",
                    this.population == eventScenario.getPopulation());
            LOGGER.info("Do we have a tripRouter? {}", tripRouter != null);

            Person walkPerson = population.getPersons().get(Id.createPersonId("person_walk"));
            List<Leg> legs = SalabimUtil.getLegs(walkPerson.getSelectedPlan());
            for (Leg leg : legs) {
                String newMode = TransportMode.car;
                // E.g. switch a person from walking to driving (and provide a route!)
                leg.setMode(newMode);
                // tripRouter.calcRoute(TransportMode.car, leg.null, null, 0, null, null);
                leg.setRoute(null);
            }
        }

    }

    private static class TrivialPopulationLegTwiddler implements StartupListener, ReplanningListener {
        @Override
        public void notifyReplanning(ReplanningEvent event) {
            twiddleLegs(event);
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            twiddleLegs(event);
        }

        void twiddleLegs(ControlerEvent event) {
            LOGGER.info("trivial leg twiddler called.");

            Scenario scenario = event.getServices().getScenario();
            Population population = scenario.getPopulation();

            for (Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
                Id<Person> id = entry.getKey();
                Person person = entry.getValue();
                // TODO here we could do something with every person
            }

            // TODO or if we know a person ID we can fetch it directly:
            Person carPerson = population.getPersons().get(Id.createPersonId("person_car"));
            List<Leg> legs = SalabimUtil.getLegs(carPerson.getSelectedPlan());
            for (Leg leg : legs) {
                // E.g. switch a person's mode to walk for all legs
                leg.setMode("walk");
                leg.setRoute(null);
            }
        }
    }
}