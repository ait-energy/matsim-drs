package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.analysis.MatchStatsControlerListener;
import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.planHandler.UndoPlans;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import at.ac.ait.matsim.domino.carpooling.planHandler.PlansModifier;

public final class CarpoolingModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(PlansModifier.class);
		addControlerListenerBinding().to(UndoPlans.class);
		addControlerListenerBinding().to(MatchStatsControlerListener.class);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(CarpoolingEngine.class).asEagerSingleton();
				addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
			}
		});
	}

}
