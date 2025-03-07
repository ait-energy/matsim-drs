package at.ac.ait.matsim.drs.dmc;

import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

import com.google.inject.Provides;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DmcForDrsModule extends AbstractDiscreteModeChoiceExtension {

    public static final String CAR_AND_DRSDRIVER = "CarAndDrsDriver";

    @Override
    protected void installExtension() {
        bindModeAvailability(CAR_AND_DRSDRIVER).to(CarAndDrsDriverModeAvailability.class);
    }

    @Provides
    public CarAndDrsDriverModeAvailability provideCarAndDrsDriverModeAvailability(DrsConfigGroup drsConfig) {
        return new CarAndDrsDriverModeAvailability(drsConfig.getDmcCarAndDrsDriverAvailableModes());
    }

}
