package at.ac.ait.matsim.domino.carpooling.analysis;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.charts.StackedBarChart;

import com.google.inject.Inject;

public class MatchStatsControlerListener implements ShutdownListener {

    @Inject
    private OutputDirectoryHierarchy output;

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        try {
            collectMatchStats(shutdownEvent);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectMatchStats(ShutdownEvent shutdownEvent) throws FileNotFoundException {
        double lastIteration = shutdownEvent.getIteration();
        BufferedWriter driversRequestMatchWriter = StatsCollector.createWriter(
                output.getOutputFilename("carpooling_driversRequestMatch.csv"),
                "Iteration,MatchedRequests,RequestsTotalNumber");
        BufferedWriter ridersRequestMatchWriter = StatsCollector.createWriter(
                output.getOutputFilename("carpooling_ridersRequestMatch.csv"),
                "Iteration,MatchedRequests,RequestsTotalNumber");
        Map<Integer, Map<String, Double>> driversMatchStatsMap = new HashMap<>();
        Map<Integer, Map<String, Double>> ridersMatchStatsMap = new HashMap<>();

        for (int i = 0; i <= lastIteration; i++) {
            Scanner scanner1 = new Scanner(new File(output.getIterationFilename(i, "carpooling_driverRequests.txt")));
            Scanner scanner2 = new Scanner(new File(output.getIterationFilename(i, "carpooling_riderRequests.txt")));
            driversMatchStatsMap.put(i, readAndSaveInfoInMap(scanner1));
            ridersMatchStatsMap.put(i, readAndSaveInfoInMap(scanner2));
        }
        for (Integer iteration : driversMatchStatsMap.keySet()) {
            int matched = 0;
            int total = 0;
            int counter = 0;
            for (Double values : driversMatchStatsMap.get(iteration).values()) {
                if (counter == 0) {
                    matched = (int) (0 + values);
                } else {
                    total = (int) (matched + values);
                }
                counter++;
            }
            try {
                driversRequestMatchWriter.write("\n" + iteration + "," + matched + "," + total);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (Integer iteration : ridersMatchStatsMap.keySet()) {
            int matched = 0;
            int total = 0;
            int counter = 0;
            for (Double values : ridersMatchStatsMap.get(iteration).values()) {
                if (counter == 0) {
                    matched = (int) (0 + values);
                } else {
                    total = (int) (matched + values);
                }
                counter++;
            }
            try {
                ridersRequestMatchWriter.write("\n" + iteration + "," + matched + "," + total);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        StatsCollector.close(driversRequestMatchWriter);
        StatsCollector.close(ridersRequestMatchWriter);
        createPng(driversMatchStatsMap, "Driver");
        createPng(ridersMatchStatsMap, "Rider");
    }

    static Map<String, Double> readAndSaveInfoInMap(Scanner scanner) {
        double totalNumberOfRequests = 0;
        double totalNumberOfMatchedRequests = 0;
        scanner.nextLine();
        Map<String, Double> matchStatsMap = new HashMap<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] fields = line.split(",");
            String isMatched = fields[7].trim();
            if (!isMatched.isEmpty()) {
                boolean matched = Boolean.parseBoolean(isMatched);
                totalNumberOfRequests++;
                if (matched) {
                    totalNumberOfMatchedRequests++;
                }
            }
        }
        matchStatsMap.put("Matched", totalNumberOfMatchedRequests);
        matchStatsMap.put("Not matched", totalNumberOfRequests - totalNumberOfMatchedRequests);
        return matchStatsMap;
    }

    private void createPng(Map<Integer, Map<String, Double>> matchStatsMap, String string) {
        Set<String> matchedAndNotMatched = matchStatsMap.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toCollection(TreeSet::new));

        String[] categories = matchStatsMap.keySet().stream()
                .map(Object::toString)
                .toArray(String[]::new);

        StackedBarChart chart = new StackedBarChart("Share of " + string + "s Matched Requests ", "Iteration",
                "Number of Requests", categories);
        chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(1, Color.RED);
        chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(0, Color.GREEN);
        for (String type : matchedAndNotMatched) {
            double[] values = matchStatsMap.values().stream()
                    .mapToDouble(map -> map.getOrDefault(type, 0.0))
                    .toArray();
            chart.addSeries(type, values);
        }

        chart.addMatsimLogo();
        chart.saveAsPng(output.getOutputFilename("carpooling_" + string + "RequestsMatch.png"), 1024, 768);
    }
}
