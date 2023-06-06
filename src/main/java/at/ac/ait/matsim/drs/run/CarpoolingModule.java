package at.ac.ait.matsim.drs.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import com.google.inject.Singleton;

import at.ac.ait.matsim.drs.analysis.RiderRequestStatsControlerListener;
import at.ac.ait.matsim.drs.analysis.VktStatsControlerListener;
import at.ac.ait.matsim.drs.engine.CarpoolingSimulationStats;
import at.ac.ait.matsim.drs.planHandler.DailyMonetaryConstantListener;
import at.ac.ait.matsim.drs.planHandler.PlanModificationUndoer;
import at.ac.ait.matsim.drs.planHandler.PlanModifier;
import at.ac.ait.matsim.drs.replanning.PermissibleModesCalculatorForCarpooling;
import at.ac.ait.matsim.drs.replanning.SubtourModeChoiceForCarpooling;

public final class CarpoolingModule extends AbstractModule {

    @Override
    public void install() {
        addControlerListenerBinding().to(DailyMonetaryConstantListener.class);
        addControlerListenerBinding().to(PlanModifier.class);
        addControlerListenerBinding().to(PlanModificationUndoer.class);
        addControlerListenerBinding().to(RiderRequestStatsControlerListener.class);
        addControlerListenerBinding().to(VktStatsControlerListener.class);

        bind(CarpoolingSimulationStats.class).in(Singleton.class);
        addControlerListenerBinding().to(CarpoolingSimulationStats.class);
        addEventHandlerBinding().to(CarpoolingSimulationStats.class);

        bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorForCarpooling.class);
        addPlanStrategyBinding(SubtourModeChoiceForCarpooling.STRATEGY_NAME)
                .toProvider(SubtourModeChoiceForCarpooling.Provider.class);

        installQSimModule(new CarpoolingEngineQSimModule());
    }

}
