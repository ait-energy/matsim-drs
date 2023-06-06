package at.ac.ait.matsim.drs.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

import at.ac.ait.matsim.drs.engine.DrsEngine;

public class DrsEngineQSimModule extends AbstractQSimModule implements QSimComponentsConfigurator {

    @Override
    public void configure(QSimComponentsConfig components) {
        components.addNamedComponent(DrsEngine.COMPONENT_NAME);
    }

    @Override
    protected void configureQSim() {
        bind(DrsEngine.class).asEagerSingleton();
        addQSimComponentBinding(DrsEngine.COMPONENT_NAME).to(DrsEngine.class);
    }

}