package at.ac.ait.matsim.drs.run;

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

import org.junit.jupiter.api.Assertions;
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

        public int size() {
            return rows.size();
        }
    }

    /**
     * Parse a gzipped CSV file and return a maping of column name to value for each
     * row
     */
    public static CSV readCsvGz(Path file, char sep) throws Exception {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            try (GZIPInputStream gis = new GZIPInputStream(fis)) {
                try (InputStreamReader isr = new InputStreamReader(gis)) {
                    try (BufferedReader br = new BufferedReader(isr)) {
                        try (CSVReaderHeaderAware csvReader = new CSVReaderHeaderAwareBuilder(br)
                                .withCSVParser(new CSVParserBuilder()
                                        .withSeparator(sep)
                                        .build())
                                .build()) {
                            List<Map<String, String>> rows = new ArrayList<>();
                            Map<String, String> kv = csvReader.readMap();
                            while (kv != null) {
                                rows.add(kv);
                                kv = csvReader.readMap();
                            }
                            return new CSV(rows);
                        }
                    }
                }
            }
        }
    }

    @Test
    @Tag("IntegrationTest")
    public void testSimpleDrsExampleWithoutExceptions(
            @TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir) {
        System.err.println("cwd:" + Path.of(".").toAbsolutePath().toString());
        System.err.println(tempDir.toAbsolutePath().toString());
        new RunSimpleDrsExample().run(true, tempDir);
    }

    @Test
    @Tag("IntegrationTest")
    public void testRidersLateForPickup(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunRidersLateForPickupExample().run(false, tempDir);
        CSV tripsCsv = readCsvGz(tempDir.resolve("output_trips.csv.gz"), ';');

        // successful DRS trip for the punctual person
        var punctual = tripsCsv.filter("person", "ridePersonPunctual").filter("trip_number", "1");
        Assertions.assertEquals(1, punctual.size());
        Assertions.assertEquals(Drs.RIDER_MODE, punctual.row(0).get("modes"));

        // successful bike + DRS trip for the cyclist
        var cyclist = tripsCsv.filter("person", "ridePersonWithBikeAccess");
        Assertions.assertEquals(2, cyclist.size());
        Assertions.assertEquals(TransportMode.bike, cyclist.row(0).get("modes"));
        Assertions.assertEquals(Drs.RIDER_MODE, cyclist.row(1).get("modes"));

        // pedestrian misses DRS (therefore no second leg)
        var pedestrian = tripsCsv.filter("person", "ridePersonWithWalkAccess");
        Assertions.assertEquals(1, pedestrian.size());
        Assertions.assertEquals(TransportMode.walk, pedestrian.row(0).get("modes"));
    }

    @Test
    @Tag("IntegrationTest")
    public void testPredefinedDrsLegs(@TempDir(cleanup = CleanupMode.NEVER, factory = TDFactory.class) Path tempDir)
            throws Exception {
        new RunPredefinedDrsLegsExample().run(false, tempDir);
    }

}
