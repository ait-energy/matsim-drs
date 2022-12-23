package at.ac.ait.matsim.domino.carpooling.run;

import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
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
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.inject.Inject;

import at.ac.ait.matsim.salabim.util.SalabimUtil;
import at.ac.ait.matsim.salabim.util.SalabimUtil.LegWithActivities;

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
        controler.addControlerListener(new FieldInjectedPopulationLegTwiddler());
        controler.run();
    }

    private static class FieldInjectedPopulationLegTwiddler implements StartupListener, ReplanningListener {

        @Inject
        private Scenario scenario;
        @Inject
        private Population population;
        @Inject
        private TripRouter tripRouter;
        // empty for now
        private Attributes routingAttributes = new Attributes();

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

            for (Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
                Id<Person> id = entry.getKey();
                Person person = entry.getValue();
                // TODO here we could do something with every person
            }

            // or we do something for specific legs of a specific person:
            Person walkPerson = population.getPersons().get(Id.createPersonId("person_walk"));
            List<PlanElement> planElements = walkPerson.getSelectedPlan().getPlanElements();
            // this fails later in the simulation, why is an exercise left to the reader ;)
            Leg legToBeReplaced = SalabimUtil.getLegsForMode(planElements, TransportMode.walk).get(0);
            replaceLegWithCarRoute(walkPerson, planElements, legToBeReplaced);
        }

        /**
         * replace a leg with a new route. note, that the provided planElements are
         * modified in place, i.e. never iterate over a person's plan and use this
         * method at the same time :)
         */
        void replaceLegWithCarRoute(Person person, List<PlanElement> planElements, Leg oldLeg) {
            LegWithActivities leg = SalabimUtil.getActivitiesForLeg(planElements, oldLeg);

            // calculate a new route
            List<? extends PlanElement> newRoute = tripRouter.calcRoute(TransportMode.car,
                    FacilitiesUtils.toFacility(leg.startActivity, scenario.getActivityFacilities()),
                    FacilitiesUtils.toFacility(leg.endActivity, scenario.getActivityFacilities()),
                    leg.startActivity.getEndTime().orElse(0), person, routingAttributes);
            // replace the previous leg with the new leg
            // or leg-activity-leg combo depending the mode!)
            TripRouter.insertTrip(planElements, leg.startActivity, newRoute, leg.endActivity);
        }

    }

}