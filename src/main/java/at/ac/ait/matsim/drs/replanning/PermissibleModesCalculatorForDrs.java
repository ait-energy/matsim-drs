package at.ac.ait.matsim.drs.replanning;

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

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class PermissibleModesCalculatorForDrs implements PermissibleModesCalculator {

    private final Set<String> availableModes;

    @Inject
    public PermissibleModesCalculatorForDrs(Config config) {
        this.availableModes = ImmutableSet.copyOf(Drs.addOrGetConfigGroup(config).getSubtourModeChoiceModes());
    }

    @Override
    public Collection<String> getPermissibleModes(final Plan plan) {
        final Person person = plan.getPerson();

        boolean hasLicense = !"no".equals(PersonUtils.getLicense(person));
        boolean carAvailable = !"never".equals(PersonUtils.getCarAvail(person));

        String affinity = DrsUtil.getCarpoolingAffinity(person);
        boolean willingToDrive = Drs.AFFINITY_DRIVER_OR_RIDER.equals(affinity)
                || Drs.AFFINITY_DRIVER_ONLY.equals(affinity);
        boolean willingToRide = Drs.AFFINITY_DRIVER_OR_RIDER.equals(affinity)
                || Drs.AFFINITY_RIDER_ONLY.equals(affinity);

        Set<String> permissibleModes = Sets.newHashSet(availableModes);
        if (!hasLicense || !carAvailable) {
            permissibleModes.remove(TransportMode.car);
        }
        if (!hasLicense || !carAvailable || !willingToDrive) {
            permissibleModes.remove(Drs.DRIVER_MODE);
        }
        if (!willingToRide) {
            permissibleModes.remove(Drs.RIDER_MODE);
        }
        return permissibleModes;
    }

}
