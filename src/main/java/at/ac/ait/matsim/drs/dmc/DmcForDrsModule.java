package at.ac.ait.matsim.drs.dmc;

import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class DmcForDrsModule extends AbstractDiscreteModeChoiceExtension {

    @Override
    protected void installExtension() {
        bindModeAvailability(CarAndDrsDriverModeAvailability.NAME).to(CarAndDrsDriverModeAvailability.class);
        bindTourConstraintFactory(DrsRiderWithInitialMeetingPointTourConstraint.NAME)
                .to(DrsRiderWithInitialMeetingPointTourConstraint.Factory.class);
    }

    @Provides
    public CarAndDrsDriverModeAvailability provideCarAndDrsDriverModeAvailability(DrsConfigGroup drsConfig) {
        return new CarAndDrsDriverModeAvailability(drsConfig.getDmcCarAndDrsDriverAvailableModes());
    }

    @Provides
    @Singleton
    public DrsRiderWithInitialMeetingPointTourConstraint.Factory provideDrsRiderWithInitialMeetingPointTourConstraintFactory(
            DiscreteModeChoiceConfigGroup dmcConfig, HomeFinder homeFinder) {
        return new DrsRiderWithInitialMeetingPointTourConstraint.Factory(homeFinder);
    }
}
