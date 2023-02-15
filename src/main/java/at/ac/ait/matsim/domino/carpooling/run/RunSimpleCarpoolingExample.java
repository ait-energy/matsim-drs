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

import at.ac.ait.matsim.domino.carpooling.Carpooling;
import at.ac.ait.matsim.salabim.util.CarFirstLinkAssigner;
import at.ac.ait.matsim.salabim.util.SalabimUtil;

public class RunSimpleCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling_simulatedPassengers.xml");
        config.network().setInputFile("network_carpooling.xml");
        config.plans().setInputFile("population_carpooling.xml");
        // config.plans().setInputFile("population_carpooling_solved.xml");

        PlanCalcScoreConfigGroup.ModeParams carpoolingDriverScore = new PlanCalcScoreConfigGroup.ModeParams(
                Carpooling.DRIVER_MODE);
        config.planCalcScore().addModeParams(carpoolingDriverScore);
        PlanCalcScoreConfigGroup.ModeParams carpoolingPassengerScore = new PlanCalcScoreConfigGroup.ModeParams(
                Carpooling.PASSENGER_MODE);
        config.planCalcScore().addModeParams(carpoolingPassengerScore);

        Set<String> networkModes = Sets.newHashSet(config.plansCalcRoute().getNetworkModes());
        networkModes.add(Carpooling.DRIVER_MODE);
        config.plansCalcRoute().setNetworkModes(Lists.newArrayList(networkModes));

        Set<String> mainModes = Sets.newHashSet(config.qsim().getMainModes());
        mainModes.add(Carpooling.DRIVER_MODE);
        config.qsim().setMainModes(Lists.newArrayList(mainModes));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add a car link to all activities with coords only
        new CarFirstLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        // add coords to all activities with links only
        SalabimUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());


        Controler controller = new Controler(scenario);

        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> {
            components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME);
        });

        controller.run();
    }
}