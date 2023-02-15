package at.ac.ait.matsim.domino.carpooling.run;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;

public class EditPopulation {
    public static void main(String[] args) {
        Population population = PopulationUtils.readPopulation("data/vienna/population.xml");
        for (Person person : population.getPersons().values()){
            for (PlanElement planElement:person.getSelectedPlan().getPlanElements()){
                if (planElement instanceof Leg){
                    if (((Leg) planElement).getMode().equals("ride")){
                        ((Leg) planElement).setMode("carpoolingPassenger");
                    } else if (((Leg) planElement).getMode().equals("car")) {
                        ((Leg) planElement).setMode("carpoolingDriver");
                    }
                }
            }
        }
        new PopulationWriter(population).write("data/vienna/population_carpooling.xml");
    }
}
