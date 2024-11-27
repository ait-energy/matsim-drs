package at.ac.ait.matsim.drs.engine;

import java.util.Objects;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DailyMonetaryConstantListener implements BeforeMobsimListener {
    private final Scenario scenario;
    private final DrsConfigGroup drsConfig;
    private final EventsManager eventsManager;

    @Inject
    public DailyMonetaryConstantListener(Scenario scenario, DrsConfigGroup drsConfig, EventsManager eventsManager) {
        this.scenario = scenario;
        this.drsConfig = drsConfig;
        this.eventsManager = eventsManager;
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
        addDailyMonetaryConstant();
    }

    private void addDailyMonetaryConstant() {
        Population population = scenario.getPopulation();
        boolean isUsingCar;
        for (Person person : population.getPersons().values()) {
            isUsingCar = false;
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (Objects.equals(((Leg) planElement).getMode(), Drs.DRIVER_MODE) ||
                            Objects.equals(((Leg) planElement).getMode(), TransportMode.car)) {
                        isUsingCar = true;
                    }
                }
            }
            if (isUsingCar) {
                eventsManager.processEvent(new PersonMoneyEvent(
                        0,
                        person.getId(),
                        drsConfig.getCarAndDrsDailyMonetaryConstant(),
                        "dailyMonetaryConstant",
                        null,
                        "drsDriver or car"));
            }
        }
    }
}
