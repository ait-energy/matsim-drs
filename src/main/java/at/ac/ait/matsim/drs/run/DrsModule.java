package at.ac.ait.matsim.drs.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import com.google.inject.Singleton;

import at.ac.ait.matsim.drs.analysis.RiderRequestStatsControlerListener;
import at.ac.ait.matsim.drs.analysis.VktStatsControlerListener;
import at.ac.ait.matsim.drs.engine.DrsSimulationStats;
import at.ac.ait.matsim.drs.planHandler.DailyMonetaryConstantListener;
import at.ac.ait.matsim.drs.planHandler.PlanModificationUndoer;
import at.ac.ait.matsim.drs.planHandler.PlanModifier;
import at.ac.ait.matsim.drs.replanning.PermissibleModesCalculatorForDrs;
import at.ac.ait.matsim.drs.replanning.SubtourModeChoiceForDrs;

public final class DrsModule extends AbstractModule {

    @Override
    public void install() {
        addControlerListenerBinding().to(DailyMonetaryConstantListener.class);
        addControlerListenerBinding().to(PlanModifier.class);
        addControlerListenerBinding().to(PlanModificationUndoer.class);
        addControlerListenerBinding().to(RiderRequestStatsControlerListener.class);
        addControlerListenerBinding().to(VktStatsControlerListener.class);

        bind(DrsSimulationStats.class).in(Singleton.class);
        addControlerListenerBinding().to(DrsSimulationStats.class);
        addEventHandlerBinding().to(DrsSimulationStats.class);

        bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorForDrs.class);
        addPlanStrategyBinding(SubtourModeChoiceForDrs.STRATEGY_NAME)
                .toProvider(SubtourModeChoiceForDrs.Provider.class);

        installQSimModule(new DrsEngineQSimModule());
    }

}
