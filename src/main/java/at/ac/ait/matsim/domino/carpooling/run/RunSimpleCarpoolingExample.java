package at.ac.ait.matsim.domino.carpooling.run;

import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import at.ac.ait.matsim.domino.carpooling.CarpoolingMode;

public class RunSimpleCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling.xml");
        config.network().setInputFile("network_carpooling.xml");
        config.plans().setInputFile("population_carpooling.xml");

        PlanCalcScoreConfigGroup.ModeParams carpoolingDriverScore = new PlanCalcScoreConfigGroup.ModeParams(
                CarpoolingMode.DRIVER);
        config.planCalcScore().addModeParams(carpoolingDriverScore);
        PlanCalcScoreConfigGroup.ModeParams carpoolingPassengerScore = new PlanCalcScoreConfigGroup.ModeParams(
                CarpoolingMode.PASSENGER);
        config.planCalcScore().addModeParams(carpoolingPassengerScore);

        Set<String> networkModes = Sets.newHashSet(config.plansCalcRoute().getNetworkModes());
        networkModes.addAll(CarpoolingMode.ALL);
        config.plansCalcRoute().setNetworkModes(Lists.newArrayList(networkModes));

        Set<String> mainModes = Sets.newHashSet(config.qsim().getMainModes());
        mainModes.addAll(CarpoolingMode.ALL);
        config.qsim().setMainModes(Lists.newArrayList(mainModes));

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controller = new Controler(scenario);

        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> {
            components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME);
        });

        controller.run();
    }
}