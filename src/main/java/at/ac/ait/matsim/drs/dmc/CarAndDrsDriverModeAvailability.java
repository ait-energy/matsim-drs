package at.ac.ait.matsim.drs.dmc;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.DefaultModeAvailability;
import org.matsim.core.population.PersonUtils;

import at.ac.ait.matsim.drs.run.Drs;

/**
 * This mode availability filters out "car" and "drsDriver" from the list of
 * given modes if an agent does not have a driving license or does not have car
 * availability.
 *
 * I.e. it is similar to CarModeAvailability but also handles drsDriver properly
 */
public class CarAndDrsDriverModeAvailability extends DefaultModeAvailability {

    public static final String NAME = "CarAndDrsDriver";

    public CarAndDrsDriverModeAvailability(Collection<String> modes) {
        super(modes);
    }

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        boolean carAvailability = !"no".equals(PersonUtils.getLicense(person));
        carAvailability &= !"never".equals(PersonUtils.getCarAvail(person));

        if (!carAvailability) {
            return super.getAvailableModes(person, trips).stream()
                    .filter(m -> !TransportMode.car.equals(m))
                    .filter(m -> !Drs.DRIVER_MODE.equals(m))
                    .collect(Collectors.toSet());
        }

        return super.getAvailableModes(person, trips);
    }
}
