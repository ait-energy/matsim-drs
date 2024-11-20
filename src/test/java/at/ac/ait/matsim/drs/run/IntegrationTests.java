package at.ac.ait.matsim.drs.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.matsim.api.core.v01.TransportMode;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;

public class IntegrationTests {

    public static class TDFactory implements TempDirFactory {
        @Override
        public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
                throws IOException {
            return Files.createTempDirectory(extensionContext.getRequiredTestMethod().getName());
        }
    }

    public static class CSV {
        public List<Map<String, String>> rows;

        public CSV(List<Map<String, String>> rows) {
            this.rows = rows;
        }

        public CSV filter(String key, String value) {
            return new CSV(rows.stream().filter(m -> m.get(key).equals(value)).collect(Collectors.toList()));
        }

        public Map<String, String> row(int i) {
            return rows.get(i);
        }

        public String get(int row, String col) {
            return rows.get(row).get(col);
        }

        public int size() {
            return rows.size();
        }
    }

    public static CSV readCsv(Path file) throws Exception {
        return readCsv(file, ';');
    }

    /**
     * Parse a CSV file (can be gzipped) and return a maping of column name to value
     * for each row
     */
    public static CSV readCsv(Path file, char sep) throws Exception {
        if (file.getFileName().toString().toLowerCase().endsWith(".gz")) {
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                try (GZIPInputStream gis = new GZIPInputStream(fis)) {
                    try (InputStreamReader isr = new InputStreamReader(gis)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            return readCsv(br, sep);
                        }
                    }
                }
            }
        }
        try (BufferedReader br = Files.newBufferedReader(file)) {
            return readCsv(br, sep);
        }
    }

    public static CSV readCsv(BufferedReader br, char sep) throws Exception {
        try (CSVReaderHeaderAware csvReader = new CSVReaderHeaderAwareBuilder(br)
                .withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build()) {
            List<Map<String, String>> rows = new ArrayList<>();
            Map<String, String> kv = csvReader.readMap();
            while (kv != null) {
                rows.add(kv);
                kv = csvReader.readMap();
            }
            return new CSV(rows);
        }
    }

    /**
     * Simply check that no exception is thrown
     * TODO replace this test.. the population.xml contains so many legacy agents
     */
    @Test
    @Tag("IntegrationTest")
    public void testSimpleDrsExample(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunSimpleDrsExample().run(true, tempDir);

        // CSV simStatsCsv = readCsv(tempDir.resolve("drs_sim_stats.csv"));
        // assertEquals(11, simStatsCsv.size());
        // assertEquals("2", simStatsCsv.get(8, "successfulPickups"));
        // assertEquals("0", simStatsCsv.get(8, "failedPickups"));
        // assertEquals("0", simStatsCsv.get(0, "stuckDrsRiders"));
        // assertEquals("0", simStatsCsv.get(0, "stuckDrsDrivers"));
    }

    @Test
    @Tag("IntegrationTest")
    public void testPredefinedDrsLegs(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunPredefinedDrsLegsExample().run(false, tempDir);
    }

    /**
     * Test DRS engine behavior: when picking up an agent the full pickup waiting
     * seconds must not be waited but departure must happen asap
     */
    @Test
    @Tag("IntegrationTest")
    public void testSinglePickupWithoutDelay(
            @TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunSinglePickupExample().run(false, tempDir);

        CSV trips = readCsv(tempDir.resolve("output_trips.csv.gz"));
        assertEquals(2, trips.size());
        // expecting pure travel time for both rider and driver,
        // the pickup waiting time must not be added
        assertEquals("00:06", trips.get(0, "trav_time").substring(0, 5));
        assertEquals("00:06", trips.get(1, "trav_time").substring(0, 5));
    }

    /**
     * Test DRS engine behavior: driver must wait pickupWaitingSeconds for late
     * riders, but leave without them if they don't show up within that time.
     */
    @Test
    @Tag("IntegrationTest")
    public void testRidersLateForPickup(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunRidersLateForPickupExample().run(false, tempDir);

        CSV riderRequestStats = readCsv(tempDir.resolve("drs_rider_request_stats.csv"));
        // we find a match for each rider
        assertEquals(1, riderRequestStats.size());
        assertEquals("3", riderRequestStats.get(0, "matched"));
        assertEquals("0", riderRequestStats.get(0, "unmatched"));

        // but one rider (pedestrian) will miss the ride
        CSV simStatsCsv = readCsv(tempDir.resolve("drs_sim_stats.csv"));
        assertEquals(1, simStatsCsv.size());
        assertEquals("2", simStatsCsv.get(0, "successfulPickups"));
        assertEquals("1", simStatsCsv.get(0, "failedPickups"));
        assertEquals("1", simStatsCsv.get(0, "stuckDrsRiders"));
        assertEquals("0", simStatsCsv.get(0, "stuckDrsDrivers"));

        CSV tripsCsv = readCsv(tempDir.resolve("output_trips.csv.gz"));

        // successful DRS trip for the punctual person
        var punctual = tripsCsv.filter("person", "ridePersonPunctual").filter("trip_number", "1");
        assertEquals(1, punctual.size());
        assertEquals(Drs.RIDER_MODE, punctual.get(0, "modes"));
        // travel time without long delays
        assertEquals("00:06", punctual.get(0, "trav_time").substring(0, 5));

        // successful bike + DRS trip for the cyclist
        var cyclist = tripsCsv.filter("person", "ridePersonWithBikeAccess");
        assertEquals(2, cyclist.size());
        assertEquals(TransportMode.bike, cyclist.get(0, "modes"));
        assertEquals(Drs.RIDER_MODE, cyclist.get(1, "modes"));
        // travel time without long delays
        assertEquals("00:06", cyclist.get(1, "trav_time").substring(0, 5));

        // pedestrian misses DRS (therefore no second leg)
        var pedestrian = tripsCsv.filter("person", "ridePersonWithWalkAccess");
        assertEquals(1, pedestrian.size());
        assertEquals(TransportMode.walk, pedestrian.get(0, "modes"));

        // all car drivers must finish their trip as well (check the distance)
        assertEquals("3737", tripsCsv.filter("person", "carPerson1").get(0, "traveled_distance"));
        assertEquals("3737", tripsCsv.filter("person", "carPerson2").get(0, "traveled_distance"));
        assertEquals("3737", tripsCsv.filter("person", "carPerson3").get(0, "traveled_distance"));
    }

    /**
     * SubtourModeChoice + Conflict logic test: Unmatched riders shouldn't get a
     * drsRider plan but an old non-conflicting plan
     */
    @Test
    @Tag("IntegrationTest")
    public void testNoDriver(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunNoDriverExample().run(false, tempDir);

        // In iteration 1 (after replanning)
        // the agent wants to use DRS but there is no driver
        CSV riderRequestStats = readCsv(tempDir.resolve("drs_rider_request_stats.csv"));
        assertEquals(2, riderRequestStats.size());
        assertEquals("0", riderRequestStats.get(1, "matched"));
        // no request should be unmatched (conflict logic must take care of it)
        assertEquals("0", riderRequestStats.get(1, "unmatched"));

        // check if conflict logic handled the bad plan
        // (one plan containing both bad requests)
        CSV conflictsCsv = readCsv(tempDir.resolve("drs_conflicts.csv"));
        assertEquals("1", conflictsCsv.get(0, "rejected_total"));

        CSV trips = readCsv(tempDir.resolve("output_trips.csv.gz"));
        // in iteration 0 bike is used (see input plan)
        assertEquals(TransportMode.bike, trips.get(0, "main_mode"));
        // in iteration 1 bike is used as well (because of the resolved conflict)
        assertEquals(TransportMode.bike, trips.get(1, "main_mode"));
    }

}
