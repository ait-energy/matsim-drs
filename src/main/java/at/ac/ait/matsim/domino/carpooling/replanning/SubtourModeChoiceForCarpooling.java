package at.ac.ait.matsim.domino.carpooling.replanning;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.run.CarpoolingConfigGroup;

/**
 * Based on {@link org.matsim.core.replanning.modules.SubtourModeChoice}
 */
public class SubtourModeChoiceForCarpooling extends AbstractMultithreadedModule {

    public static final String STRATEGY_NAME = "SubtourModeChoiceForCarpooling";

    private final PermissibleModesCalculator permissibleModesCalculator;
    private final String[] chainBasedModes;
    private final String[] modes;

    public SubtourModeChoiceForCarpooling(Config config,
            PermissibleModesCalculator permissibleModesCalculator) {
        super(config.global().getNumberOfThreads());
        CarpoolingConfigGroup carpoolingConfig = Carpooling.addOrGetConfigGroup(config);
        this.modes = carpoolingConfig.getSubtourModeChoiceModes().clone();
        this.chainBasedModes = carpoolingConfig.getSubtourModeChoiceChainBasedModes().clone();
        this.permissibleModesCalculator = permissibleModesCalculator;
    }

    protected String[] getModes() {
        return modes.clone();
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        SubtourModeChoice.Behavior behavior = SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes;
        double probaForChooseRandomSingleTripMode = 0;
        ChooseRandomLegModeForSubtour chooseRandomLegMode = new ChooseRandomLegModeForSubtour(
                TripStructureUtils.getRoutingModeIdentifier(),
                this.permissibleModesCalculator,
                this.modes,
                this.chainBasedModes,
                MatsimRandom.getLocalInstance(),
                behavior,
                probaForChooseRandomSingleTripMode);
        return chooseRandomLegMode;
    }

    public static class Provider implements javax.inject.Provider<PlanStrategy> {

        private static final Logger LOGGER = LogManager.getLogger();

        @Inject
        private Config config;
        @Inject
        private PermissibleModesCalculator permissibleModesCalculator;

        @Override
        public PlanStrategy get() {
            LOGGER.info("Provider builds a new instance of {}", SubtourModeChoiceForCarpooling.STRATEGY_NAME);
            PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<>());
            builder.addStrategyModule(new SubtourModeChoiceForCarpooling(config, permissibleModesCalculator));
            return builder.build();
        }

    }

}
