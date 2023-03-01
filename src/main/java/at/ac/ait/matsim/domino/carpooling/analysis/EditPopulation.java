package at.ac.ait.matsim.domino.carpooling.analysis;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.Objects;
import java.util.Random;

public class EditPopulation {
    public static void main(String[] args) {
        Population population = PopulationUtils.readPopulation("data/vienna/population.xml");
        Population testPopulation = PopulationUtils.createPopulation(ConfigUtils.loadConfig("data/vienna/config_carpooling.xml"));
        Random random = new Random(0);

        for (Person person : population.getPersons().values()){
            for (PlanElement planElement:person.getSelectedPlan().getPlanElements()){
                if (planElement instanceof Leg){
                    if (((Leg) planElement).getMode().equals("ride")){
                        if (random.nextDouble()<0.1){
                            ((Leg) planElement).setMode("carpoolingRider");
                        }
                    } else if (((Leg) planElement).getMode().equals("car")) {
                        if (random.nextDouble()<0.1){
                            ((Leg) planElement).setMode("carpoolingDriver");
                        }
                    }
                }
            }
        }

        for (Person person : population.getPersons().values()){
            boolean condition = false;
            if (person.getSelectedPlan().getPlanElements().size()>5){
                for (PlanElement planElement:person.getSelectedPlan().getPlanElements() ){
                    if (planElement instanceof Leg){
                        if (Objects.equals(((Leg) planElement).getMode(), "carpoolingDriver")) {
                           condition =true;
                        }
                    }
                }
                if (condition){
                    if (random.nextDouble()>0.99){ testPopulation.addPerson(person);}}
            }
        }

        System.out.println(testPopulation.getPersons().size());
        PopulationUtils.writePopulation(testPopulation,"data/vienna/testPopulation.xml");
    }
}
