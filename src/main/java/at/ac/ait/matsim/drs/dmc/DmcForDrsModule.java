package at.ac.ait.matsim.drs.dmc;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import com.google.inject.Provides;

public class DmcForDrsModule extends AbstractDiscreteModeChoiceExtension {

    public static final String CAR_AND_DRSDRIVER = "CarAndDrsDriver";

    @Override
    protected void installExtension() {
        bindModeAvailability(CAR_AND_DRSDRIVER).to(CarAndDrsDriverModeAvailability.class);
    }

    @Provides
    public CarAndDrsDriverModeAvailability provideCarAndDrsDriverModeAvailability(
            DiscreteModeChoiceConfigGroup dmcConfig) {
        // FIXME we must retrieve this from our own config group
        // ModeAvailabilityConfigGroup config = (ModeAvailabilityConfigGroup)
        // dmcConfig.getComponentConfig(
        // DiscreteModeChoiceConfigGroup.MODE_AVAILABILITY,
        // CAR_AND_DRSDRIVER);
        // return new CarAndDrsDriverModeAvailability(config.getAvailableModes());
        return new CarAndDrsDriverModeAvailability(List.of(TransportMode.car));
    }

}
