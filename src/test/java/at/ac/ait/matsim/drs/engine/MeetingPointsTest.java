package at.ac.ait.matsim.drs.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import at.ac.ait.matsim.drs.run.DrsConfigGroup;

public class MeetingPointsTest {

    private static final String configFile = "data/floridsdorf/config_drs.xml";
    private static final String meetingPointsFile = "data/floridsdorf/meeting_points.gpkg";

    @Test
    public void testLoadAndMatchMeetingPoints() {
        Config config = ConfigUtils.loadConfig(configFile, new DrsConfigGroup(), new DiscreteModeChoiceConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Map<String, ActivityFacility> meetingPoints = MeetingPoints.loadAndMatchMeetingPoints(scenario,
                meetingPointsFile);

        assertEquals(4, meetingPoints.size());

        var missingId = meetingPoints.get("missingId1");
        assertNotNull(missingId, "expect id to filled in");
        assertEquals("501", missingId.getLinkId().toString(),
                "no linkId given, must be matched to nearest link");

        assertTrue(Set.of("1121", "1122").contains(meetingPoints.get("72").getLinkId().toString()),
                "invalid link id given, must be matched to nearest link");

        assertTrue(Set.of("1121", "1122").contains(meetingPoints.get("72_dup1").getLinkId().toString()),
                "invalid link id given, must be matched to nearest link");

        assertEquals("1281", meetingPoints.get("157").getLinkId().toString(), "valid link id given, must be kept");
    }
}
