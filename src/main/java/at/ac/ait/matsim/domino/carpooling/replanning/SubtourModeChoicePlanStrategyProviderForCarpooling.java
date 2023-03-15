package at.ac.ait.matsim.domino.carpooling.replanning;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class SubtourModeChoicePlanStrategyProviderForCarpooling implements Provider<PlanStrategy> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    private GlobalConfigGroup globalConfigGroup;
    @Inject
    private PermissibleModesCalculator permissibleModesCalculator;

    @Override
    public PlanStrategy get() {
        LOGGER.info("providing the extended plan strategy!");
        PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<>());
        builder.addStrategyModule(new SubtourModeChoiceForCarpooling(globalConfigGroup,
                permissibleModesCalculator));
        // builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider,
        // globalConfigGroup, timeInterpretation));
        return builder.build();
    }

}
