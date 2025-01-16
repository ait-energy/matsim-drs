package at.ac.ait.matsim.drs.run.tmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.NameImpl;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * meeting points:
 * point layer in geo file with attributes "id (string)" optionally "linkId
 * (string)")
 * for each point: either the referenced link or the nearest link are used.
 *
 * The fact that MATSim ignores turning restrictions helps us here: matching a
 * single link at complex motorway junctions is sufficient to (often) get
 * realistic travel times for routes in all directions
 *
 * We must restrict ourselves to one link per meeting point because
 * RoutingModules only allow routing from/to facilities
 * (i.e. specific directed links) and also
 * LeastCostPathCalculators internally route from/to points (a single one)
 */
public class NetworkPointFinder {

    public static String LAYER_NAME = "meeting_points";

    private Scenario scenario;
    private Controler controller;
    private RoutingModule carRouter;

    public NetworkPointFinder(String configFileName) {
        Config config = ConfigUtils.loadConfig(configFileName, new DrsConfigGroup());
        scenario = ScenarioUtils.loadScenario(config);

        // prepare controller so that we can retrieve routers etc
        controller = new Controler(scenario);
        controller.getInjector();

        TripRouter tripRouter = controller.getTripRouterProvider().get();
        carRouter = tripRouter.getRoutingModule(TransportMode.car);
    }

    public void demo(String meetingPointsFile) {
        Collection<ActivityFacility> meetingPoints = loadMeetingPoints(meetingPointsFile);
        for (ActivityFacility from : meetingPoints) {
            System.out.println(from);
            for (ActivityFacility to : meetingPoints) {
                if (from == to) {
                    continue;
                }
                System.out.println(from.getId() + " --> " + to.getId());
                printRoute(from, to);
            }
        }
    }

    private Collection<ActivityFacility> loadMeetingPoints(String fileName) {
        GeoFileReader geoFileReader = new GeoFileReader();
        Collection<SimpleFeature> features = geoFileReader.readFileAndInitialize(fileName, new NameImpl(LAYER_NAME));

        ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
        Map<String, ActivityFacility> meetingPoints = new HashMap<>();
        for (SimpleFeature feature : features) {
            Object idAttr = feature.getAttribute("id");
            if (idAttr == null) {
                throw new IllegalArgumentException("meeting point without id: " + feature.getDefaultGeometry());
            }
            String idStr = idAttr.toString();
            if (meetingPoints.keySet().contains(idStr)) {
                throw new IllegalArgumentException("meeting point id is not unique: " + idStr);
            }

            Object linkIdAttr = feature.getAttribute("linkId");
            Point point = (Point) feature.getDefaultGeometry();
            Coord coord = new Coord(point.getX(), point.getY());

            Link link = null;
            if (linkIdAttr != null) {
                link = scenario.getNetwork().getLinks().get(Id.createLinkId(linkIdAttr.toString()));
            }
            if (link == null) {
                link = NetworkUtils.getNearestLink(scenario.getNetwork(), coord);
            }
            if (link == null) {
                throw new IllegalArgumentException("could not determine link for meeting point " + idStr);
            }
            meetingPoints.put(idStr,
                    factory.createActivityFacility(Id.create(idStr, ActivityFacility.class), coord,
                            link.getId()));
        }
        return meetingPoints.values();
    }

    private void printRoute(Facility from, Facility to) {
        // Link fromLink = scenario.getNetwork().getLinks().get(Id.createLinkId(from));
        // Link toLink = scenario.getNetwork().getLinks().get(Id.createLinkId(to));
        Person driver = scenario.getPopulation().getPersons().values().iterator().next();
        Leg leg = DrsUtil.calculateLeg(carRouter, from, to, 0, driver);

        List<Id<Link>> allLinks = new ArrayList<>();
        allLinks.add(leg.getRoute().getStartLinkId());
        allLinks.addAll(((NetworkRoute) leg.getRoute()).getLinkIds());
        allLinks.add(leg.getRoute().getEndLinkId());
        System.out.println(allLinks);
    }

    public static void main(String[] args) {
        // new
        // NetworkPointFinder("data/floridsdorf/config_drs.xml").demo("data/floridsdorf/meeting_points.shp");
        new NetworkPointFinder("data/vienna/config_drs.xml").demo("data/vienna/meeting_points.gpkg");
    }

}
