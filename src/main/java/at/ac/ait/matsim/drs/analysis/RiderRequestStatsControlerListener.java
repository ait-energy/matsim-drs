package at.ac.ait.matsim.drs.analysis;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;
import com.google.inject.Inject;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class RiderRequestStatsControlerListener implements AfterMobsimListener {

    public static final String FILENAME_REQUEST_STATS = "drs_rider_request_stats";
    public static final String MATCHED = Drs.REQUEST_STATUS_MATCHED;
    public static final String NOT_MATCHED = "unmatched";
    private final Population population;
    private final String requestFileName;
    private final boolean createPNG;
    Map<Integer, Map<String, Integer>> iterationHistories = new HashMap<>();
    private final Map<String, Integer> requestCount = new TreeMap<>();

    @Inject
    public RiderRequestStatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population,
            OutputDirectoryHierarchy controlerIO) {
        this.population = population;
        this.requestFileName = controlerIO.getOutputFilename(FILENAME_REQUEST_STATS);
        this.createPNG = controlerConfigGroup.isCreateGraphs();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        this.collectRequestStatsInfo(afterMobsimEvent);
    }

    private void collectRequestStatsInfo(AfterMobsimEvent event) {
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (Objects.equals(((Leg) planElement).getMode(), Drs.RIDER_MODE)) {
                        if (Objects.equals(DrsUtil.getRequestStatus((Leg) planElement), MATCHED)) {
                            if (requestCount.get(MATCHED) == null) {
                                this.requestCount.put(MATCHED, 1);
                            } else {
                                this.requestCount.put(MATCHED, requestCount.get(MATCHED) + 1);
                            }
                        } else {
                            if (requestCount.get(NOT_MATCHED) == null) {
                                this.requestCount.put(NOT_MATCHED, 1);
                            } else {
                                this.requestCount.put(NOT_MATCHED, requestCount.get(NOT_MATCHED) + 1);
                            }
                        }
                    }
                }
            }
        }

        Map<String, Integer> requestHistory = new HashMap<>();
        if (requestCount.get(MATCHED) == null) {
            requestHistory.put(MATCHED, 0);
        } else {
            requestHistory.put(MATCHED, requestCount.get(MATCHED));
        }
        if (requestCount.get(NOT_MATCHED) == null) {
            requestHistory.put(NOT_MATCHED, 0);
        } else {
            requestHistory.put(NOT_MATCHED, requestCount.get(NOT_MATCHED));
        }
        this.iterationHistories.put(event.getIteration(), requestHistory);

        BufferedWriter requestOut = IOUtils.getBufferedWriter(this.requestFileName + ".txt");

        try {
            requestOut.write("Iteration");
            requestOut.write("\t" + MATCHED);
            requestOut.write("\t" + NOT_MATCHED);
            requestOut.write("\n");

            for (int iteration = 0; iteration <= event.getIteration(); ++iteration) {
                requestOut.write(String.valueOf(iteration));

                Map<String, Integer> matchedMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + matchedMap.get(MATCHED));

                Map<String, Integer> notMatchedMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + notMatchedMap.get(NOT_MATCHED));

                requestOut.write("\n");
            }
            requestOut.flush();
            requestOut.close();
        } catch (IOException var1) {
            var1.printStackTrace();
            throw new UncheckedIOException(var1);
        }

        if (this.createPNG) {
            String[] categories = iterationHistories.keySet().stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
            StackedBarChart chart = new StackedBarChart("Drs Rider Request Statistics", "iteration", "requests",
                    categories);

            double[] matchedValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(MATCHED, 0))
                    .toArray();
            chart.addSeries(MATCHED, matchedValues);

            double[] notMatchedValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(NOT_MATCHED, 0))
                    .toArray();
            chart.addSeries(NOT_MATCHED, notMatchedValues);

            chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(0, Color.GREEN);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(1, Color.RED);
            chart.addMatsimLogo();
            chart.saveAsPng(this.requestFileName + ".png", 800, 600);
        }
        this.requestCount.clear();
    }
}
