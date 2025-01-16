package at.ac.ait.matsim.drs.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.replanning.conflicts.ConflictModule;

import com.google.inject.Singleton;

import at.ac.ait.matsim.drs.analysis.DrsReplanningStats;
import at.ac.ait.matsim.drs.analysis.DrsSimStats;
import at.ac.ait.matsim.drs.analysis.VktStatsControlerListener;
import at.ac.ait.matsim.drs.dmc.DmcForDrsModule;
import at.ac.ait.matsim.drs.engine.DailyMonetaryConstantListener;
import at.ac.ait.matsim.drs.engine.DrsData;
import at.ac.ait.matsim.drs.engine.DrsReplanningListener;
import at.ac.ait.matsim.drs.engine.PermissibleModesCalculatorForDrs;
import at.ac.ait.matsim.drs.engine.PlanModificationUndoer;
import at.ac.ait.matsim.drs.engine.UnmatchedRiderConflictIdentifier;

public final class DrsModule extends AbstractModule {

    @Override
    public void install() {
        install(new DmcForDrsModule());

        addControlerListenerBinding().to(DailyMonetaryConstantListener.class);
        addControlerListenerBinding().to(DrsReplanningListener.class);
        addControlerListenerBinding().to(PlanModificationUndoer.class);
        addControlerListenerBinding().to(VktStatsControlerListener.class);

        bind(DrsReplanningStats.class).in(Singleton.class);
        bind(DrsSimStats.class).in(Singleton.class);
        addControlerListenerBinding().to(DrsSimStats.class);
        addEventHandlerBinding().to(DrsSimStats.class);

        bind(DrsData.class).asEagerSingleton();

        bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorForDrs.class);

        installQSimModule(new DrsEngineQSimModule());

        // bind the conflict identifier here so that
        // WorstPlanForRemovalSelectorWithConflicts (or other removal strategies)
        // know which plans can have conflicts
        ConflictModule.bindResolver(binder()).toInstance(new UnmatchedRiderConflictIdentifier());
    }

}
