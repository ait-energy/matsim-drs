package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class Carpooling {
    public static final String DRIVER_MODE = "carpoolingDriver";
    public static final String RIDER_MODE = "carpoolingRider";
    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";
    public static final String RIDER_INTERACTION = RIDER_MODE + " interaction";
    public static final String ATTRIB_ORIGINAL_DEP_TIME = "originalDepTime";
    public static final String ATTRIB_LINKED_REQUEST = "linkedRequest";
    public static final String ATTRIB_RIDER_ID = "riderId";
    public static final String ATTRIB_ACTIVITY_TYPE = "type";
    public static final String ATTRIB_REQUEST_STATUS = "requestStatus";
    public static final String ATTRIB_CARPOOLING_STATUS = "carpoolingStatus";
    public static final String ATTRIB_AFFINITY = "carpoolingAffinity";
    public static final String AFFINITY_DRIVER_OR_RIDER = "driverOrRider";
    public static final String AFFINITY_DRIVER_ONLY = "driverOnly";
    public static final String AFFINITY_RIDER_ONLY = "riderOnly";

    public enum ActivityType {
        pickup, dropoff
    }

    public static CarpoolingConfigGroup addOrGetConfigGroup(Scenario scenario) {
        return ConfigUtils.addOrGetModule(scenario.getConfig(), CarpoolingConfigGroup.GROUP_NAME,
                CarpoolingConfigGroup.class);
    }

    public static CarpoolingConfigGroup addOrGetConfigGroup(Config config) {
        return ConfigUtils.addOrGetModule(config, CarpoolingConfigGroup.GROUP_NAME, CarpoolingConfigGroup.class);
    }

    public static void prepareController(Controler controller) {
        controller.addOverridingModule(new CarpoolingModule());
        controller.configureQSimComponents(components -> {
            new CarpoolingEngineQSimModule().configure(components);
        });
    }

}
