package at.ac.ait.matsim.domino.carpooling;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import at.ac.ait.matsim.domino.carpooling.run.CarpoolingEngine;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingModule;
import at.ac.ait.matsim.salabim.util.CarFirstLinkAssigner;
import at.ac.ait.matsim.salabim.util.SalabimUtil;

public class Carpooling {
    public static final String DRIVER_MODE = "carpoolingDriver";
    public static final String PASSENGER_MODE = "carpoolingPassenger";

    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";
    public static final String PASSENGER_INTERACTION = PASSENGER_MODE + " interaction";

    public static final String PASSENGER_ID_ATTRIB = "passengerId";
    public static final String ACTIVITY_TYPE_ATTRIB = "type";

    public static enum ActivityType {
        pickup, dropoff
    };

    public static Id<Person> getPassengerId(Activity activity) {
        Object id = activity.getAttributes().getAttribute(PASSENGER_ID_ATTRIB);
        return id == null ? null : Id.createPersonId(id.toString());
    }

    public static void setPassengerId(Activity activity, Id<Person> id) {
        if (id != null) {
            activity.getAttributes().putAttribute(PASSENGER_ID_ATTRIB, id.toString());
        } else {
            activity.getAttributes().removeAttribute(PASSENGER_ID_ATTRIB);
        }
    }

    public static ActivityType getActivityType(Activity activity) {
        Object type = activity.getAttributes().getAttribute(ACTIVITY_TYPE_ATTRIB);
        return type == null ? null : ActivityType.valueOf(type.toString());
    }

    public static void setActivityType(Activity activity, ActivityType type) {
        if (type != null) {
            activity.getAttributes().putAttribute(ACTIVITY_TYPE_ATTRIB, type.toString());
        } else {
            activity.getAttributes().removeAttribute(ACTIVITY_TYPE_ATTRIB);
        }
    }

    public static void prepareConfig(Config config) {
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
    }

    public static void prepareScenario(Scenario scenario) {
        // add a car link to all activities with coords only
        new CarFirstLinkAssigner(scenario.getNetwork()).run(scenario.getPopulation());
        // add coords to all activities with links only
        SalabimUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        Carpooling.addCarpoolingDriverToCarLinks(scenario.getNetwork());
    }

    /** adds carpooling driver mode to all car links */
    public static void addCarpoolingDriverToCarLinks(Network network) {
        network.getLinks().values().forEach(l -> {
            if (l.getAllowedModes().contains(TransportMode.car)) {
                Set<String> modes = Sets.newHashSet(l.getAllowedModes());
                modes.add(Carpooling.DRIVER_MODE);
                l.setAllowedModes(modes);
            }
        });
    }

    public static void prepareController(Controler controller) {
        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> {
            components.addNamedComponent(CarpoolingEngine.COMPONENT_NAME);
        });
    }

}
