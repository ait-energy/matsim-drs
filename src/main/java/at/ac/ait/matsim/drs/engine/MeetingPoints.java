package at.ac.ait.matsim.drs.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.NameImpl;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;

/**
 * Util to load meeting points as specified in the DrsConfigGroup.
 *
 * We must restrict ourselves to one link per meeting point because
 * RoutingModules only allow routing from/to facilities
 * (i.e. specific directed links) and also
 * LeastCostPathCalculators internally route from/to points (a single one)
 *
 * The fact that MATSim ignores turning restrictions helps us here: matching a
 * single link at complex motorway junctions is sufficient to (often) get
 * realistic travel times for routes in all directions
 */
public class MeetingPoints {

    public static String LAYER_NAME = "meeting_points";
    public static String MISSING_ID_PREFIX = "missingId";

    private static final Logger LOGGER = LogManager.getLogger();

    public static Map<String, ActivityFacility> loadAndMatchMeetingPoints(Scenario scenario, String fileName) {
        GeoFileReader geoFileReader = new GeoFileReader();
        Collection<SimpleFeature> features = geoFileReader.readFileAndInitialize(fileName, new NameImpl(LAYER_NAME));

        ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
        Map<String, ActivityFacility> meetingPoints = new HashMap<>();
        int missingId = 0;
        int matchingFail = 0;
        for (SimpleFeature feature : features) {
            Object idAttr = feature.getAttribute("id");
            if (idAttr == null) {
                missingId++;
                idAttr = MISSING_ID_PREFIX + missingId;
            }
            String idStr = idAttr.toString();
            if (meetingPoints.keySet().contains(idStr)) {
                LOGGER.warn("fixing duplicate ID {}", idStr);
                int idAppendix = 0;
                String fixedId;
                do {
                    idAppendix++;
                    fixedId = idStr + "_dup" + idAppendix;
                } while (meetingPoints.keySet().contains(fixedId));
                idStr = fixedId;
            }

            Object linkIdAttr = feature.getAttribute("linkId");
            Geometry geom = (Geometry) feature.getDefaultGeometry();
            Point point;
            if (geom instanceof Point) {
                point = (Point) geom;
            } else {
                point = geom.getCentroid();
            }
            Coord coord = new Coord(point.getX(), point.getY());

            Link link = null;
            if (linkIdAttr != null) {
                link = scenario.getNetwork().getLinks().get(Id.createLinkId(linkIdAttr.toString()));
                if (link == null) {
                    LOGGER.warn("Could not find link {} for meeting point {}. Assigning closest link.", linkIdAttr,
                            idStr);
                }
            }
            if (link == null) {
                link = NetworkUtils.getNearestLink(scenario.getNetwork(), coord);
            }
            if (link == null) {
                LOGGER.warn("Could not find a link for meeting point {}. Removing it.", idStr);
                matchingFail++;
            } else {
                meetingPoints.put(idStr,
                        factory.createActivityFacility(Id.create(idStr, ActivityFacility.class), coord,
                                link.getId()));
            }
        }

        LOGGER.info("Successfully initialized {}/{} meeting points.", meetingPoints.size(),
                (meetingPoints.size() + matchingFail));
        return meetingPoints;
    }

}
