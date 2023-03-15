package at.ac.ait.matsim.domino.carpooling.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import com.google.inject.Inject;

public class PermissibleModesCalculatorForCarpooling implements PermissibleModesCalculator {
    private final List<String> availableModes;
    private final List<String> availableModesWithoutCar;
    private final boolean considerCarAvailability;

    @Inject
    public PermissibleModesCalculatorForCarpooling(Config config) {
        this.availableModes = Arrays.asList(config.subtourModeChoice().getModes());

        if (this.availableModes.contains(TransportMode.car)) {
            final List<String> l = new ArrayList<String>(this.availableModes);
            while (l.remove(TransportMode.car)) {
            }
            this.availableModesWithoutCar = Collections.unmodifiableList(l);
        } else {
            this.availableModesWithoutCar = this.availableModes;
        }

        this.considerCarAvailability = config.subtourModeChoice().considerCarAvailability();
    }

    @Override
    public Collection<String> getPermissibleModes(final Plan plan) {
        if (!considerCarAvailability)
            return availableModes;

        final Person person;
        try {
            person = plan.getPerson();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("I need a PersonImpl to get car availability");
        }

        final boolean carAvail = !"no".equals(PersonUtils.getLicense(person)) &&
                !"never".equals(PersonUtils.getCarAvail(person));

        return carAvail ? availableModes : availableModesWithoutCar;
    }

}
