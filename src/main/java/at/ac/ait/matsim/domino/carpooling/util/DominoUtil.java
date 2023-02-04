package at.ac.ait.matsim.domino.carpooling.util;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripStructureUtils;

public class DominoUtil {

    public static Leg getFirstLeg(List<? extends PlanElement> planElements) {
        List<Leg> legs = TripStructureUtils.getLegs(planElements);
        if (legs.size() != 1) {
            throw new IllegalStateException("expected exactly one leg but got " + legs.size());
        }
        return legs.get(0);
    }

}
