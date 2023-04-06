package at.ac.ait.matsim.domino.carpooling.planHandler;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import java.util.*;

public class DailyMonetaryConstantListener implements BeforeMobsimListener {
    private final Scenario scenario;
    private final CarpoolingConfigGroup cfgGroup;
    private final EventsManager eventsManager;

    @Inject
    public DailyMonetaryConstantListener(Scenario scenario, EventsManager eventsManager) {
        this.scenario = scenario;
        cfgGroup = Carpooling.addOrGetConfigGroup(scenario);
        this.eventsManager = eventsManager;
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) { addDailyMonetaryConstant();}
    private void addDailyMonetaryConstant() {
        Population population = scenario.getPopulation();
        boolean isUsingCar;
        for (Person person : population.getPersons().values()) {
            isUsingCar =false;
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (Objects.equals(((Leg) planElement).getMode(), Carpooling.DRIVER_MODE) ||
                            Objects.equals(((Leg) planElement).getMode(), TransportMode.car) ) {
                        isUsingCar=true;
                    }
                }
            }
            if (isUsingCar){
                eventsManager.processEvent(new PersonMoneyEvent(0, person.getId(),
                        -cfgGroup.getCarAndCarpoolingDailyMonetaryConstant(), "dailyMonetaryConstant",
                        null, "carpoolingDriver or car"));
            }
        }
    }
}
