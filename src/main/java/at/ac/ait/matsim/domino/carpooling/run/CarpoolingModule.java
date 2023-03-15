package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;

import at.ac.ait.matsim.domino.carpooling.analysis.PlanElementsStatsListener;
import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.planHandler.PlansModifier;
import at.ac.ait.matsim.domino.carpooling.planHandler.UndoPlans;
import at.ac.ait.matsim.domino.carpooling.replanning.ExtendedSubtourModeChoice;
import at.ac.ait.matsim.domino.carpooling.replanning.ExtendedSubtourModeChoicePlanStrategyProvider;

public final class CarpoolingModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(PlansModifier.class);
		addControlerListenerBinding().to(UndoPlans.class);
		addControlerListenerBinding().to(PlanElementsStatsListener.class);

		// TODO create our own implementation
		bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorImpl.class);
		addPlanStrategyBinding(ExtendedSubtourModeChoice.STRATEGY_NAME)
				.toProvider(ExtendedSubtourModeChoicePlanStrategyProvider.class);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(CarpoolingEngine.class).asEagerSingleton();
				addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
			}
		});
	}

}
