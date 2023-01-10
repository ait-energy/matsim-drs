package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.planModifier.CarpoolingDriverPlanModifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.Arrays;

public class RunCarpoolingExample {

    //TODO: Check how Scoring is working. Check how teleporting is working for passenger. Check how aborting is working for passenger. Increase the population size and see how it is working! Check if it is possible to match using the routes of the driver.
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_vanilla.xml");

        //Edit the network by adding the modes "carpoolingDriver" and "carpoolingPassenger" to the links that has car as allowed mode
        config.network().setInputFile("CarpoolingSimpleNetwork.xml");
        config.plans().setInputFile("CarpoolingSimplePopulation.xml");

        //Configure the scoring for the new modes and carpooling interaction activity
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
        PlanCalcScoreConfigGroup.ActivityParams carpoolingInteraction = new PlanCalcScoreConfigGroup.ActivityParams("carpoolingInteraction");
        carpoolingInteraction.setTypicalDuration(120);
        config.planCalcScore().addActivityParams(carpoolingInteraction);

        config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ) );
        config.qsim().setMainModes(Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.addControlerListener(new CarpoolingDriverPlanModifier());
        controler.run();
    }
}