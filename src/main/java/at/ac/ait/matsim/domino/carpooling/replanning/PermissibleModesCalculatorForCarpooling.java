package at.ac.ait.matsim.domino.carpooling.replanning;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class PermissibleModesCalculatorForCarpooling implements PermissibleModesCalculator {

    private final Set<String> availableModes;

    @Inject
    public PermissibleModesCalculatorForCarpooling(Config config) {
        this.availableModes = ImmutableSet.copyOf(Carpooling.addOrGetConfigGroup(config).getSubtourModeChoiceModes());
    }

    @Override
    public Collection<String> getPermissibleModes(final Plan plan) {
        final Person person = plan.getPerson();

        boolean hasLicense = !"no".equals(PersonUtils.getLicense(person));
        boolean carAvailable = !"never".equals(PersonUtils.getCarAvail(person));

        String affinity = CarpoolingUtil.getCarpoolingAffinity(person);
        boolean willingToDrive = Carpooling.AFFINITY_DRIVER_OR_RIDER.equals(affinity)
                || Carpooling.AFFINITY_DRIVER_ONLY.equals(affinity);
        boolean willingToRide = Carpooling.AFFINITY_DRIVER_OR_RIDER.equals(affinity)
                || Carpooling.AFFINITY_RIDER_ONLY.equals(affinity);

        Set<String> permissibleModes = Sets.newHashSet(availableModes);
        if (!hasLicense || !carAvailable) {
            permissibleModes.remove(TransportMode.car);
        }
        if (!hasLicense || !carAvailable || !willingToDrive) {
            permissibleModes.remove(Carpooling.DRIVER_MODE);
        }
        if (!willingToRide) {
            permissibleModes.remove(Carpooling.RIDER_MODE);
        }
        return permissibleModes;
    }

}
