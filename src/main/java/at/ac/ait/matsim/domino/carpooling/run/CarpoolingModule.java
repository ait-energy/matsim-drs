package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import at.ac.ait.matsim.domino.carpooling.analysis.MatchStatsControlerListener;
import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.planHandler.PlansModifier;
import at.ac.ait.matsim.domino.carpooling.planHandler.UndoPlans;
import at.ac.ait.matsim.domino.carpooling.replanning.PermissibleModesCalculatorForCarpooling;
import at.ac.ait.matsim.domino.carpooling.replanning.SubtourModeChoiceForCarpooling;

public final class CarpoolingModule extends AbstractModule {
    @Override
    public void install() {
        addControlerListenerBinding().to(PlansModifier.class);
        addControlerListenerBinding().to(UndoPlans.class);
        addControlerListenerBinding().to(MatchStatsControlerListener.class);

        bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorForCarpooling.class);
        addPlanStrategyBinding(SubtourModeChoiceForCarpooling.STRATEGY_NAME)
                .toProvider(SubtourModeChoiceForCarpooling.Provider.class);

        installQSimModule(new AbstractQSimModule() {
            @Override
            protected void configureQSim() {
                bind(CarpoolingEngine.class).asEagerSingleton();
                addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
            }
        });
    }

}
