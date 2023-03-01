package at.ac.ait.matsim.domino.carpooling.run;

import java.util.Set;

import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.scoring.CarpoolingScoringFunctionFactory;
import at.ac.ait.matsim.domino.carpooling.util.CarFirstLinkAssigner;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Carpooling {
    public static final String DRIVER_MODE = "carpoolingDriver";
    public static final String RIDER_MODE = "carpoolingRider";

    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";
    public static final String RIDER_INTERACTION = RIDER_MODE + " interaction";
    public static final String ORIGINAL_DEP_TIME = "originalDepTime";
    public static final String LINKED_REQUEST = "linkedRequest";
    public static final String RIDER_ID_ATTRIB = "riderId";
    public static final String ACTIVITY_TYPE_ATTRIB = "type";

    public enum ActivityType {
        pickup, dropoff
    }

    public static void prepareConfig(Config config) {
        PlanCalcScoreConfigGroup.ModeParams carpoolingDriverScore = new PlanCalcScoreConfigGroup.ModeParams(
                Carpooling.DRIVER_MODE);
        config.planCalcScore().addModeParams(carpoolingDriverScore);
        PlanCalcScoreConfigGroup.ModeParams carpoolingRiderScore = new PlanCalcScoreConfigGroup.ModeParams(
                Carpooling.RIDER_MODE);
        config.planCalcScore().addModeParams(carpoolingRiderScore);

        Set<String> networkModes = Sets.newHashSet(config.plansCalcRoute().getNetworkModes());
        networkModes.add(Carpooling.DRIVER_MODE);
        config.plansCalcRoute().setNetworkModes(Lists.newArrayList(networkModes));

        Set<String> mainModes = Sets.newHashSet(config.qsim().getMainModes());
        mainModes.add(Carpooling.DRIVER_MODE);
        config.qsim().setMainModes(Lists.newArrayList(mainModes));
    }

    public static void prepareScenario(Scenario scenario) {
        new CarFirstLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        CarpoolingUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        CarpoolingUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Carpooling.DRIVER_MODE);
    }

    public static void prepareController(Controler controller) {
        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME));
        /*controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindScoringFunctionFactory().to(CarpoolingScoringFunctionFactory.class);
            }
        });*/
    }
}
