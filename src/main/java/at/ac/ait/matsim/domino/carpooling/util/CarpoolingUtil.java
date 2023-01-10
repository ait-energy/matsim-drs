package at.ac.ait.matsim.domino.carpooling.util;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;
import java.util.stream.Collectors;

public class CarpoolingUtil {
    public static List<Leg> getLegs(Plan plan) {
        return getLegs(plan.getPlanElements());
    }

    public static List<Leg> getLegs(List<? extends PlanElement> elements) {
        return elements.stream() //
                .filter(Leg.class::isInstance) //
                .map(Leg.class::cast) //
                .collect(Collectors.toList());
    }
    public static class LegWithActivities {
        public final Activity startActivity;
        public final Leg leg;
        public final Activity endActivity;

        public LegWithActivities(Activity startActivity, Leg leg, Activity endActivity) {
            this.startActivity = startActivity;
            this.leg = leg;
            this.endActivity = endActivity;
        }

    }

    public static CarpoolingUtil.LegWithActivities getActivitiesForLeg(List<? extends PlanElement> elements, Leg leg) {
        int i = elements.indexOf(leg);
        return new CarpoolingUtil.LegWithActivities((Activity) elements.get(i - 1), leg, (Activity) elements.get(i + 1));
    }

}
