package at.ac.ait.matsim.drs.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Rejects plans containing one or more unmatched drs rider legs
 */
public class UnmatchedRiderConflictResolver extends UnmatchedRiderConflictIdentifier {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public IdSet<Person> resolve(Population population, int iteration) {
        IdSet<Person> conflictingPersonIds = new IdSet<>(Person.class);
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (Leg leg : TripStructureUtils.getLegs(plan)) {
                if (leg.getMode().equals(Drs.RIDER_MODE)) {
                    String requestStatus = DrsUtil.getRequestStatus(leg);
                    if (requestStatus == null || !requestStatus.equals(Drs.REQUEST_STATUS_MATCHED)) {
                        LOGGER.info(
                                "Unmatched rider '{}': conflict module tries to select a non-conflicting plan",
                                person.getId());
                        conflictingPersonIds.add(person.getId());
                        break;
                    }
                }
            }
        }
        return conflictingPersonIds;
    }

}
