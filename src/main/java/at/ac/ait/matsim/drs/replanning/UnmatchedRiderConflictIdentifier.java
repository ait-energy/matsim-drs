package at.ac.ait.matsim.drs.replanning;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.conflicts.ConflictResolver;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.run.Drs;

public class UnmatchedRiderConflictIdentifier implements ConflictResolver {

    @Override
    public IdSet<Person> resolve(Population population, int iteration) {
        return new IdSet<>(Person.class, 0);
    }

    @Override
    public boolean isPotentiallyConflicting(Plan plan) {
        for (Leg leg : TripStructureUtils.getLegs(plan)) {
            if (leg.getMode().equals(Drs.RIDER_MODE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
