package at.ac.ait.matsim.domino.carpooling.run;

import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import at.ac.ait.matsim.domino.carpooling.engine.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.util.CarFirstLinkAssigner;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class Carpooling {
    public static final String DRIVER_MODE = "carpoolingDriver";
    public static final String RIDER_MODE = "carpoolingRider";
    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";
    public static final String RIDER_INTERACTION = RIDER_MODE + " interaction";
    public static final String ATTRIB_ORIGINAL_DEP_TIME = "originalDepTime";
    public static final String ATTRIB_LINKED_REQUEST = "linkedRequest";
    public static final String ATTRIB_RIDER_ID = "riderId";
    public static final String ATTRIB_ACTIVITY_TYPE = "type";
    public static final  String ATTRIB_REQUEST_STATUS = "requestStatus" ;
    public static final String ATTRIB_AFFINITY = "carpoolingAffinity";
    public static final String AFFINITY_DRIVER_OR_RIDER = "driverOrRider";
    public static final String AFFINITY_DRIVER_ONLY = "driverOnly";
    public static final String AFFINITY_RIDER_ONLY = "riderOnly";


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

    public static CarpoolingConfigGroup addOrGetConfigGroup(Scenario scenario) {
        return ConfigUtils.addOrGetModule(scenario.getConfig(), CarpoolingConfigGroup.GROUP_NAME,
                CarpoolingConfigGroup.class);
    }

    public static CarpoolingConfigGroup addOrGetConfigGroup(Config config) {
        return ConfigUtils.addOrGetModule(config, CarpoolingConfigGroup.GROUP_NAME, CarpoolingConfigGroup.class);
    }

    public static void prepareScenario(Scenario scenario) {
        new CarFirstLinkAssigner(scenario.getNetwork(), 500).run(scenario.getPopulation());
        CarpoolingUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        CarpoolingUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), Carpooling.DRIVER_MODE);
        CarpoolingUtil.addDriverPlanForEligibleAgents(scenario.getPopulation(), scenario.getConfig());
    }

    public static void prepareController(Controler controller) {
        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME));
    }
}
