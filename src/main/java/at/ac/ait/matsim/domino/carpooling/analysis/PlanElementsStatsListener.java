package at.ac.ait.matsim.domino.carpooling.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

public class PlanElementsStatsListener implements IterationEndsListener{
    final private BufferedWriter out= IOUtils.getBufferedWriter("output/planElementsStats.txt");
    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        collectStats(event);
    }
    private void collectStats(final IterationEndsEvent event) {
        Scenario eventScenario = event.getServices().getScenario();
        Population population = eventScenario.getPopulation();
        int totalNumberOfPlans= 0;
        for (Person person : population.getPersons().values()) {
          totalNumberOfPlans = totalNumberOfPlans + person.getSelectedPlan().getPlanElements().size();
        }

        try {
            this.out.write(event.getIteration() + "\t" + totalNumberOfPlans+ "\n");
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
