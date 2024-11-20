package at.ac.ait.matsim.drs.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class Drs {
    public static final String DRIVER_MODE = "drsDriver";
    public static final String RIDER_MODE = "drsRider";
    public static final String DRIVER_INTERACTION = DRIVER_MODE + " interaction";

    public static final String ATTRIB_ORIGINAL_DEP_TIME = "originalDepTime";
    public static final String ATTRIB_RIDER_ID = "riderId";
    public static final String ATTRIB_ACTIVITY_TYPE = "type";
    /**
     * status of a drs rider leg after the optimization/matching phase,
     * i.e. planned / scheduled matching of drivers to riders
     */
    public static final String ATTRIB_ASSIGNED_DRIVER = "drsAssignedDriver";

    /**
     * status of a drs driver leg after qsim,
     * i.e. if picking up riders was successful during qsim
     */
    public static final String ATTRIB_DRS_STATUS = "drsStatus";
    public static final String VALUE_STATUS_DRS = "drs";
    public static final String VALUE_STATUS_BEFORE_AFTER = "beforeAndAfterDrs";

    public static final String ATTRIB_AFFINITY = "drsAffinity";
    public static final String AFFINITY_DRIVER_OR_RIDER = "driverOrRider";
    public static final String AFFINITY_DRIVER_ONLY = "driverOnly";
    public static final String AFFINITY_RIDER_ONLY = "riderOnly";

    public enum ActivityType {
        pickup, dropoff
    }

    public static DrsConfigGroup addOrGetConfigGroup(Scenario scenario) {
        return ConfigUtils.addOrGetModule(scenario.getConfig(), DrsConfigGroup.GROUP_NAME,
                DrsConfigGroup.class);
    }

    public static DrsConfigGroup addOrGetConfigGroup(Config config) {
        return ConfigUtils.addOrGetModule(config, DrsConfigGroup.GROUP_NAME, DrsConfigGroup.class);
    }

    public static void prepareController(Controler controller) {
        controller.addOverridingModule(new DrsModule());
        controller.configureQSimComponents(components -> {
            new DrsEngineQSimModule().configure(components);
        });
    }

}
