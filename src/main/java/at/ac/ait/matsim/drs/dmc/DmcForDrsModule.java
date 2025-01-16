package at.ac.ait.matsim.drs.dmc;

import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.ModeAvailabilityConfigGroup;

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
        ModeAvailabilityConfigGroup config = dmcConfig.getCarModeAvailabilityConfig();
        return new CarAndDrsDriverModeAvailability(config.getAvailableModes());
    }

}
