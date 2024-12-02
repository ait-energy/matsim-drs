package at.ac.ait.matsim.drs.analysis;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.optimizer.MatchingResult;
import at.ac.ait.matsim.drs.util.DrsUtil;

/**
 * Stats of the results of the Drs replanning (i.e. optimization)
 */
public class DrsReplanningStats {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String FILENAME = "drs_replanning_stats";

    public static enum CsvField {
        matchedDrivers, unmatchedDrivers, matchedRiders, unmatchedRiders
    };

    private static Map<CsvField, Color> FIELD2COLOR = Map.of(
            CsvField.matchedDrivers, new Color(34, 34, 85),
            CsvField.unmatchedDrivers, new Color(187, 204, 238),
            CsvField.matchedRiders, new Color(102, 102, 51),
            CsvField.unmatchedRiders, new Color(238, 238, 187));

    private final String statsFileName;
    private final String delimiter;
    private final ControllerConfigGroup controllerConfigGroup;
    Map<Integer, Map<CsvField, Integer>> iteration2stats = new HashMap<>();

    @Inject
    public DrsReplanningStats(ControllerConfigGroup controllerConfigGroup,
            OutputDirectoryHierarchy controllerIO,
            GlobalConfigGroup globalConfigGroup) {
        this.statsFileName = controllerIO.getOutputFilename(FILENAME);
        this.delimiter = globalConfigGroup.getDefaultDelimiter();
        this.controllerConfigGroup = controllerConfigGroup;
    }

    public void writeStats(int iteration, int driverRequests, int riderRequests, MatchingResult result) {
        boolean driverMismatch = result.unmatchedDriverRequests().size() + result.matches().size() != driverRequests;
        boolean riderMismatch = result.unmatchedRiderRequests().size() + result.matches().size() != riderRequests;
        if (driverMismatch || riderMismatch) {
            LOGGER.error("Drs request mismatch. This hints at a bug in the drs module, investigate!");
        }

        Map<CsvField, Integer> stats = Map.of(
                CsvField.matchedDrivers, result.matches().size(),
                CsvField.unmatchedDrivers, result.unmatchedDriverRequests().size(),
                CsvField.matchedRiders, result.matches().size(),
                CsvField.unmatchedRiders, result.unmatchedRiderRequests().size());
        iteration2stats.put(iteration, stats);

        writeCsv();
        if (DrsUtil.writeGraph(iteration, controllerConfigGroup)) {
            writePng();
        }
    }

    private void writeCsv() {
        try (BufferedWriter requestOut = IOUtils.getBufferedWriter(this.statsFileName + ".csv")) {
            requestOut.write("Iteration");
            for (CsvField field : CsvField.values()) {
                requestOut.write(delimiter + field);
            }
            requestOut.write("\n");

            for (int iteration : iteration2stats.keySet()) {
                requestOut.write(String.valueOf(iteration));
                for (CsvField field : CsvField.values()) {
                    requestOut.write(delimiter + iteration2stats.get(iteration).get(field));
                }
                requestOut.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    private void writePng() {
        String[] categories = iteration2stats.keySet().stream()
                .map(Object::toString)
                .toArray(String[]::new);
        StackedBarChart chart = new StackedBarChart("Drs Request Statistics",
                "Iteration", "Requests",
                categories);

        for (CsvField field : CsvField.values()) {
            double[] values = iteration2stats.values().stream()
                    .mapToDouble(map -> map.getOrDefault(field, 0))
                    .toArray();
            chart.addSeries(field.toString(), values);
        }

        chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        int i = 0;
        for (CsvField field : CsvField.values()) {
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(i, FIELD2COLOR.get(field));
            i++;
        }
        chart.addMatsimLogo();
        chart.saveAsPng(this.statsFileName + ".png", 800, 600);
    }

}
