package at.ac.ait.matsim.domino.carpooling.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripStructureUtils;

/**
 * Based on {@link org.matsim.core.replanning.modules.SubtourModeChoice}
 */
public class ExtendedSubtourModeChoice extends AbstractMultithreadedModule {

    public static final String STRATEGY_NAME = "ExtendedSubtourModeChoice";

    private final PermissibleModesCalculator permissibleModesCalculator;

    private final String[] chainBasedModes;
    private final String[] modes;

    public ExtendedSubtourModeChoice(GlobalConfigGroup globalConfigGroup,
            PermissibleModesCalculator permissibleModesCalculator) {
        this(globalConfigGroup.getNumberOfThreads(),
                new String[] { "car", "bike", "pt", "walk" },
                new String[] { "car" },
                permissibleModesCalculator);
    }

    ExtendedSubtourModeChoice(
            final int numberOfThreads,
            final String[] modes,
            final String[] chainBasedModes,
            PermissibleModesCalculator permissibleModesCalculator) {
        super(numberOfThreads);
        this.modes = modes.clone();
        this.chainBasedModes = chainBasedModes.clone();
        this.permissibleModesCalculator = permissibleModesCalculator;
    }

    protected String[] getModes() {
        return modes.clone();
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {

        final ExtendedChooseRandomLegModeForSubtour chooseRandomLegMode = new ExtendedChooseRandomLegModeForSubtour(
                TripStructureUtils.getRoutingModeIdentifier(),
                this.permissibleModesCalculator,
                this.modes,
                this.chainBasedModes,
                MatsimRandom.getLocalInstance());
        return chooseRandomLegMode;
    }

}
