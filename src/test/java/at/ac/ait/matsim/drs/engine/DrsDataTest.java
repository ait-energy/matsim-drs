package at.ac.ait.matsim.drs.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DrsDataTest {

    @Test
    public void testFindH3Resolution() {
        assertEquals(9, DrsData.findH3ResolutionForDistance(150));
        assertEquals(8, DrsData.findH3ResolutionForDistance(400));
        assertEquals(7, DrsData.findH3ResolutionForDistance(500));
        assertEquals(7, DrsData.findH3ResolutionForDistance(1200));
    }
}
