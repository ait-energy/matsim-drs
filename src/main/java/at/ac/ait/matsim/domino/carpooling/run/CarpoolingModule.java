package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.analysis.RiderRequestStatsControlerListener;
import at.ac.ait.matsim.domino.carpooling.analysis.VktStatsControlerListener;
import at.ac.ait.matsim.domino.carpooling.planHandler.DailyMonetaryConstantListener;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.planHandler.PlanModifier;
import at.ac.ait.matsim.domino.carpooling.planHandler.PlanModificationUndoer;
import at.ac.ait.matsim.domino.carpooling.replanning.PermissibleModesCalculatorForCarpooling;
import at.ac.ait.matsim.domino.carpooling.replanning.SubtourModeChoiceForCarpooling;

public final class CarpoolingModule extends AbstractModule {
    @Override
    public void install() {
        addControlerListenerBinding().to(DailyMonetaryConstantListener.class);
        addControlerListenerBinding().to(PlanModifier.class);
        addControlerListenerBinding().to(PlanModificationUndoer.class);
        addControlerListenerBinding().to(RiderRequestStatsControlerListener.class);
        addControlerListenerBinding().to(VktStatsControlerListener.class);

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
