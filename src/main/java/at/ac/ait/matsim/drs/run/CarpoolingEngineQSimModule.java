package at.ac.ait.matsim.drs.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

import at.ac.ait.matsim.drs.engine.CarpoolingEngine;

public class CarpoolingEngineQSimModule extends AbstractQSimModule implements QSimComponentsConfigurator {

    @Override
    public void configure(QSimComponentsConfig components) {
        components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME);
    }

    @Override
    protected void configureQSim() {
        bind(CarpoolingEngine.class).asEagerSingleton();
        addQSimComponentBinding(CarpoolingEngine.COMPONENT_NAME).to(CarpoolingEngine.class);
    }

}