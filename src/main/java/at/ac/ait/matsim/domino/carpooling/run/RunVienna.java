package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.driver.CarpoolingDriverPlanModifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;

public class RunVienna {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/vienna/config_vanilla.xml");
        config.plans().setInputFile("population_carpooling.xml");
        config.network().setInputFile("network_carpooling.xml");

        PlanCalcScoreConfigGroup.ModeParams carpoolingDriverScore = new PlanCalcScoreConfigGroup.ModeParams("carpoolingDriver");
        carpoolingDriverScore.setMode("carpoolingDriver");
        carpoolingDriverScore.setConstant(0);
        carpoolingDriverScore.setMarginalUtilityOfDistance(0);
        carpoolingDriverScore.setMarginalUtilityOfTraveling(0);
        config.planCalcScore().addModeParams(carpoolingDriverScore);
        PlanCalcScoreConfigGroup.ModeParams carpoolingPassengerScore = new PlanCalcScoreConfigGroup.ModeParams("carpoolingPassenger");
        carpoolingPassengerScore.setMode("carpoolingPassenger");
        carpoolingPassengerScore.setConstant(0);
        carpoolingPassengerScore.setMarginalUtilityOfDistance(0);
        carpoolingPassengerScore.setMarginalUtilityOfTraveling(0);
        config.planCalcScore().addModeParams(carpoolingPassengerScore);

        config.plansCalcRoute().setNetworkModes(Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ) );
        config.qsim().setMainModes(Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.addControlerListener(new CarpoolingDriverPlanModifier());
        controler.run();
    }

}
