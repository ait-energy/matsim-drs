package at.ac.ait.matsim.drs.dmc;

import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class DmcForDrsModule extends AbstractDiscreteModeChoiceExtension {

    public static final String CAR_AND_DRSDRIVER = "CarAndDrsDriver";

    @Override
    protected void installExtension() {
        bindModeAvailability(CAR_AND_DRSDRIVER).to(CarAndDrsDriverModeAvailability.class);
    }

}
