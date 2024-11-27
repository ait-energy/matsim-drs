package at.ac.ait.matsim.drs.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.replanning.conflicts.ConflictModule;

import com.google.inject.Singleton;

import at.ac.ait.matsim.drs.analysis.RiderRequestStatsControlerListener;
import at.ac.ait.matsim.drs.analysis.VktStatsControlerListener;
import at.ac.ait.matsim.drs.engine.DailyMonetaryConstantListener;
import at.ac.ait.matsim.drs.engine.DrsData;
import at.ac.ait.matsim.drs.engine.DrsSimulationStats;
import at.ac.ait.matsim.drs.engine.PermissibleModesCalculatorForDrs;
import at.ac.ait.matsim.drs.engine.PlanModificationUndoer;
import at.ac.ait.matsim.drs.engine.PlanModifier;
import at.ac.ait.matsim.drs.engine.SubtourModeChoiceForDrs;
import at.ac.ait.matsim.drs.engine.UnmatchedRiderConflictIdentifier;

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

        bind(DrsData.class).asEagerSingleton();

        bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorForDrs.class);
        addPlanStrategyBinding(SubtourModeChoiceForDrs.STRATEGY_NAME)
                .toProvider(SubtourModeChoiceForDrs.Provider.class);

        installQSimModule(new DrsEngineQSimModule());

        // bind the conflict identifier here so that
        // WorstPlanForRemovalSelectorWithConflicts (or other removal strategies)
        // know which plans can have conflicts
        ConflictModule.bindResolver(binder()).toInstance(new UnmatchedRiderConflictIdentifier());
    }

}
