package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import at.ac.ait.matsim.domino.carpooling.driver.CarpoolingDriverPlanModifier;

public final class CarpoolingModule extends AbstractModule {

	@Override
	public void install() {
		// TODO re-enable!! (deactivated for test purposes)
		addControlerListenerBinding().to(CarpoolingDriverPlanModifier.class);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(CarpoolingEngine.class).asEagerSingleton();
				addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
			}
		});
	}

}
