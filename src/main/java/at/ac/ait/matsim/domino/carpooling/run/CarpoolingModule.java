package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import at.ac.ait.matsim.domino.carpooling.planModifier.DriverPlanModifier;

public final class CarpoolingModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(DriverPlanModifier.class);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(CarpoolingEngine.class).asEagerSingleton();
				addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
			}
		});
	}

}
