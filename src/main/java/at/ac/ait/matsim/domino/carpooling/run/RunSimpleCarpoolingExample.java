package at.ac.ait.matsim.domino.carpooling.run;

import at.ac.ait.matsim.domino.carpooling.planModifier.CarpoolingDriverPlanModifier;
import org.apache.commons.compress.utils.Sets;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.Arrays;

public class RunSimpleCarpoolingExample {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("data/floridsdorf/config_carpooling.xml");
        //createNetworkAndPopulation(config);
        config.network().setInputFile("network_carpooling.xml");
        config.plans().setInputFile("CarpoolingSimplePopulationOld.xml");

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

        config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ) );
        config.qsim().setMainModes(Arrays.asList( TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.addControlerListener(new CarpoolingDriverPlanModifier());
        controler.run();
    }
     static void createNetworkAndPopulation(Config config){
        Network network = NetworkUtils.createNetwork();
        NetworkFactory networkFactory = network.getFactory();

        Node node0 = networkFactory.createNode(Id.createNodeId(0), new Coord(0, 0));
        network.addNode(node0);
        Node node1 = networkFactory.createNode(Id.createNodeId(1), new Coord(1000, 0));
        network.addNode(node1);
        Node node2 = networkFactory.createNode(Id.createNodeId(2), new Coord(2000, 0));
        network.addNode(node2);
        Node node3 = networkFactory.createNode(Id.createNodeId(3), new Coord(3000, 0));
        network.addNode(node3);
        Node node4 = networkFactory.createNode(Id.createNodeId(4), new Coord(4000, 0));
        network.addNode(node4);
        Node node5 = networkFactory.createNode(Id.createNodeId(5), new Coord(5000, 0));
        network.addNode(node5);
        Node node6 = networkFactory.createNode(Id.createNodeId(6), new Coord(2000, 1000));
        network.addNode(node6);
        Node node7 = networkFactory.createNode(Id.createNodeId(7), new Coord(2000, 2000));
        network.addNode(node7);
        Node node8 = networkFactory.createNode(Id.createNodeId(8), new Coord(3000, 1000));
        network.addNode(node8);
        Node node9 = networkFactory.createNode(Id.createNodeId(9), new Coord(3000, 2000));
        network.addNode(node9);
        Node node10 = networkFactory.createNode(Id.createNodeId(10), new Coord(3000, 10000));
        network.addNode(node10);
        Node node11 = networkFactory.createNode(Id.createNodeId(11), new Coord(2000, -10000));
        network.addNode(node11);


        double linkCapacity = 10;

        Link link01 = networkFactory.createLink(Id.createLinkId("0_1"), node0, node1);
        link01.setCapacity(linkCapacity);
        link01.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link01);
        Link link10 = networkFactory.createLink(Id.createLinkId("1_0"), node1, node0);
        link10.setCapacity(linkCapacity);
        link10.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link10);
        Link link12 = networkFactory.createLink(Id.createLinkId("1_2"), node1, node2);
        link12.setCapacity(linkCapacity);
        link12.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link12);
        Link link21 = networkFactory.createLink(Id.createLinkId("2_1"), node2, node1);
        link21.setCapacity(linkCapacity);
        link21.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link21);
        Link link23 = networkFactory.createLink(Id.createLinkId("2_3"), node2, node3);
        link23.setCapacity(linkCapacity);
        link23.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link23);
        Link link32 = networkFactory.createLink(Id.createLinkId("3_2"), node3, node2);
        link32.setCapacity(linkCapacity);
        link32.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link32);
        Link link34 = networkFactory.createLink(Id.createLinkId("3_4"), node3, node4);
        link34.setCapacity(linkCapacity);
        link34.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link34);
        Link link43 = networkFactory.createLink(Id.createLinkId("4_3"), node4, node3);
        link43.setCapacity(linkCapacity);
        link43.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link43);
        Link link45 = networkFactory.createLink(Id.createLinkId("4_5"), node4, node5);
        link45.setCapacity(linkCapacity);
        link45.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link45);
        Link link54 = networkFactory.createLink(Id.createLinkId("5_4"), node5, node4);
        link54.setCapacity(linkCapacity);
        link54.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link54);
        Link link26 = networkFactory.createLink(Id.createLinkId("2_6"), node2, node6);
        link26.setCapacity(linkCapacity);
        link26.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link26);
        Link link62 = networkFactory.createLink(Id.createLinkId("6_2"), node6, node2);
        link62.setCapacity(linkCapacity);
        link62.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link62);
        Link link67 = networkFactory.createLink(Id.createLinkId("6_7"), node6, node7);
        link67.setCapacity(linkCapacity);
        link67.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link67);
        Link link76 = networkFactory.createLink(Id.createLinkId("7_6"), node7, node6);
        link76.setCapacity(linkCapacity);
        link76.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link76);
        Link link38 = networkFactory.createLink(Id.createLinkId("3_8"), node3, node8);
        link38.setCapacity(linkCapacity);
        link38.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link38);
        Link link83 = networkFactory.createLink(Id.createLinkId("8_3"), node8, node3);
        link83.setCapacity(linkCapacity);
        link83.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link83);
        Link link89 = networkFactory.createLink(Id.createLinkId("8_9"), node8, node9);
        link89.setCapacity(linkCapacity);
        link89.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link89);
        Link link98 = networkFactory.createLink(Id.createLinkId("9_8"), node9, node8);
        link98.setCapacity(linkCapacity);
        link98.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link98);
        Link link910 = networkFactory.createLink(Id.createLinkId("9_10"), node9, node10);
        link910.setCapacity(linkCapacity);
        link910.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link910);
        Link link109 = networkFactory.createLink(Id.createLinkId("10_9"), node10, node9);
        link109.setCapacity(linkCapacity);
        link109.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link109);
        Link link211 = networkFactory.createLink(Id.createLinkId("2_11"), node2, node11);
        link211.setCapacity(linkCapacity);
        link211.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link211);
        Link link112 = networkFactory.createLink(Id.createLinkId("11_2"), node11, node2);
        link112.setCapacity(linkCapacity);
        link112.setAllowedModes(Sets.newHashSet(TransportMode.car,"carpoolingPassenger","carpoolingDriver" ));
        network.addLink(link112);

        new NetworkWriter(network).write("data/floridsdorf/CarpoolingSimpleNetwork.xml");


        Population population = PopulationUtils.createPopulation(config);
        PopulationFactory populationFactory = population.getFactory();

        //Car driver
        Person person1 = populationFactory.createPerson(Id.createPersonId("person1_carDriver"));
        Plan person1plan = populationFactory.createPlan();
        Activity person1activity1 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person1activity1.setEndTime(4*60*60);
        person1plan.addActivity(person1activity1);
        Leg person1leg1 = populationFactory.createLeg(TransportMode.car);
        person1plan.addLeg(person1leg1);
        Activity person1activity2 = populationFactory.createActivityFromLinkId("work",link45.getId());
        person1activity2.setStartTime(4*60*60+45*60);
        person1activity2.setEndTime(12*60*60);
        person1plan.addActivity(person1activity2);
        Leg person1leg2 = populationFactory.createLeg(TransportMode.car);
        person1plan.addLeg(person1leg2);
        Activity person1activity3 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person1activity3.setStartTime(12*60*60+45*60);
        person1plan.addActivity(person1activity3);
        person1.addPlan(person1plan);
        population.addPerson(person1);

        //Carpooling driver with successful match
        Person person2 = populationFactory.createPerson(Id.createPersonId("person2_carpoolingDriver"));
        Plan person2plan = populationFactory.createPlan();
        Activity person2activity1 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person2activity1.setEndTime(5*60*60);
        person2plan.addActivity(person2activity1);
        Leg person2leg1 = populationFactory.createLeg("carpoolingDriver");
        person2plan.addLeg(person2leg1);
        Activity person2activity2 = populationFactory.createActivityFromLinkId("work",link45.getId());
        person2activity2.setStartTime(5*60*60+45*60);
        person2activity2.setEndTime(13*60*60);
        person2plan.addActivity(person2activity2);
        Leg person2leg2 = populationFactory.createLeg("carpoolingDriver");
        person2plan.addLeg(person2leg2);
        Activity person2activity3 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person2activity3.setStartTime(13*60*60+45*60);
        person2plan.addActivity(person2activity3);
        person2.addPlan(person2plan);
        population.addPerson(person2);

        //Carpooling driver with no match due to spatial reasons
        Person person3 = populationFactory.createPerson(Id.createPersonId("person3_carpoolingDriver"));
        Plan person3plan = populationFactory.createPlan();
        Activity person3activity1 = populationFactory.createActivityFromLinkId("home",link910.getId());
        person3activity1.setEndTime(5*60*60);
        person3plan.addActivity(person3activity1);
        Leg person3leg1 = populationFactory.createLeg("carpoolingDriver");
        person3plan.addLeg(person3leg1);
        Activity person3activity2 = populationFactory.createActivityFromLinkId("work",link211.getId());
        person3activity2.setStartTime(5*60*60+45*60);
        person3activity2.setEndTime(13*60*60);
        person3plan.addActivity(person3activity2);
        Leg person3leg2 = populationFactory.createLeg("carpoolingDriver");
        person3plan.addLeg(person3leg2);
        Activity person3activity3 = populationFactory.createActivityFromLinkId("home",link910.getId());
        person3activity3.setStartTime(13*60*60+45*60);
        person3plan.addActivity(person3activity3);
        person3.addPlan(person3plan);
        population.addPerson(person3);

        //Carpooling driver with no match due to temporal reasons
        Person person4 = populationFactory.createPerson(Id.createPersonId("person4_carpoolingDriver"));
        Plan person4plan = populationFactory.createPlan();
        Activity person4activity1 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person4activity1.setEndTime(3*60*60);
        person4plan.addActivity(person4activity1);
        Leg person4leg1 = populationFactory.createLeg("carpoolingDriver");
        person4plan.addLeg(person4leg1);
        Activity person4activity2 = populationFactory.createActivityFromLinkId("work",link45.getId());
        person4activity2.setStartTime(3*60*60+45*60);
        person4activity2.setEndTime(11*60*60);
        person4plan.addActivity(person4activity2);
        Leg person4leg2 = populationFactory.createLeg("carpoolingDriver");
        person4plan.addLeg(person4leg2);
        Activity person4activity3 = populationFactory.createActivityFromLinkId("home",link10.getId());
        person4activity3.setStartTime(11*60*60+45*60);
        person4plan.addActivity(person4activity3);
        person4.addPlan(person4plan);
        population.addPerson(person4);

        //Carpooling passenger fit to match with person2_carpoolingDriver
        Person person5 = populationFactory.createPerson(Id.createPersonId("person5_carpoolingPassenger"));
        Plan person5plan = populationFactory.createPlan();
        Activity person5activity1 = populationFactory.createActivityFromLinkId("home",link26.getId());
        person5activity1.setEndTime(5*60*60);
        person5plan.addActivity(person5activity1);
        Leg person5leg1 = populationFactory.createLeg("carpoolingPassenger");
        person5plan.addLeg(person5leg1);
        Activity person5activity2 = populationFactory.createActivityFromLinkId("work",link38.getId());
        person5activity2.setStartTime(5*60*60+45*60);
        person5activity2.setEndTime(13*60*60);
        person5plan.addActivity(person5activity2);
        Leg person5leg2 = populationFactory.createLeg("carpoolingPassenger");
        person5plan.addLeg(person5leg2);
        Activity person5activity3 = populationFactory.createActivityFromLinkId("home",link26.getId());
        person5activity3.setStartTime(13*60*60+45*60);
        person5plan.addActivity(person5activity3);
        person5.addPlan(person5plan);
        population.addPerson(person5);

        //Carpooling passenger fit to match with person2_carpoolingDriver but with higher detour
        Person person6 = populationFactory.createPerson(Id.createPersonId("person6_carpoolingPassenger"));
        Plan person6plan = populationFactory.createPlan();
        Activity person6activity1 = populationFactory.createActivityFromLinkId("home",link67.getId());
        person6activity1.setEndTime(5*60*60);
        person6plan.addActivity(person6activity1);
        Leg person6leg1 = populationFactory.createLeg("carpoolingPassenger");
        person6plan.addLeg(person6leg1);
        Activity person6activity2 = populationFactory.createActivityFromLinkId("work",link38.getId());
        person6activity2.setStartTime(5*60*60+45*60);
        person6activity2.setEndTime(13*60*60);
        person6plan.addActivity(person6activity2);
        Leg person6leg2 = populationFactory.createLeg("carpoolingPassenger");
        person6plan.addLeg(person6leg2);
        Activity person6activity3 = populationFactory.createActivityFromLinkId("home",link67.getId());
        person6activity3.setStartTime(13*60*60+45*60);
        person6plan.addActivity(person6activity3);
        person6.addPlan(person6plan);
        population.addPerson(person6);

        //Carpooling passenger with no match due to spatial reasons
        Person person7 = populationFactory.createPerson(Id.createPersonId("person7_carpoolingPassenger"));
        Plan person7plan = populationFactory.createPlan();
        Activity person7activity1 = populationFactory.createActivityFromLinkId("home",link211.getId());
        person7activity1.setEndTime(5*60*60);
        person7plan.addActivity(person7activity1);
        Leg person7leg1 = populationFactory.createLeg("carpoolingPassenger");
        person7plan.addLeg(person7leg1);
        Activity person7activity2 = populationFactory.createActivityFromLinkId("work",link910.getId());
        person7activity2.setStartTime(5*60*60+45*60);
        person7activity2.setEndTime(13*60*60);
        person7plan.addActivity(person7activity2);
        Leg person7leg2 = populationFactory.createLeg("carpoolingPassenger");
        person7plan.addLeg(person7leg2);
        Activity person7activity3 = populationFactory.createActivityFromLinkId("home",link211.getId());
        person7activity3.setStartTime(13*60*60+45*60);
        person7plan.addActivity(person7activity3);
        person7.addPlan(person7plan);
        population.addPerson(person7);

        //Carpooling passenger with no match due to temporal reasons
        Person person8 = populationFactory.createPerson(Id.createPersonId("person8_carpoolingPassenger"));
        Plan person8plan = populationFactory.createPlan();
        Activity person8activity1 = populationFactory.createActivityFromLinkId("home",link26.getId());
        person8activity1.setEndTime(8*60*60);
        person8plan.addActivity(person8activity1);
        Leg person8leg1 = populationFactory.createLeg("carpoolingPassenger");
        person8plan.addLeg(person8leg1);
        Activity person8activity2 = populationFactory.createActivityFromLinkId("work",link38.getId());
        person8activity2.setStartTime(8*60*60+45*60);
        person8activity2.setEndTime(16*60*60);
        person8plan.addActivity(person8activity2);
        Leg person8leg2 = populationFactory.createLeg("carpoolingPassenger");
        person8plan.addLeg(person8leg2);
        Activity person8activity3 = populationFactory.createActivityFromLinkId("home",link26.getId());
        person8activity3.setStartTime(16*60*60+45*60);
        person8plan.addActivity(person8activity3);
        person8.addPlan(person8plan);
        population.addPerson(person8);

        PopulationWriter writer = new PopulationWriter(population);
        writer.write("data/floridsdorf/CarpoolingSimplePopulation.xml");

    }
}