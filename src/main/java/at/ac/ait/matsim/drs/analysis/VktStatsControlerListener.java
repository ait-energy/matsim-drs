package at.ac.ait.matsim.drs.analysis;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class VktStatsControlerListener implements AfterMobsimListener {
    public static final String FILENAME_VKT_STATS = "carpooling_vkt_stats";
    public static final String CARPOOLING_TRAVEL = "carpooling travel";
    public static final String INDIVIDUAL_TRAVEL = "individual travel";
    public static final String BEFORE_AFTER_CARPOOLING_TRAVEL = "before and after carpooling";
    private final Population population;
    private final String requestFileName;
    private final boolean createPNG;
    private final Map<Integer, Map<String, Double>> iterationHistories = new HashMap<>();

    @Inject
    public VktStatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population,
            OutputDirectoryHierarchy controlerIO) {
        this.population = population;
        this.requestFileName = controlerIO.getOutputFilename(FILENAME_VKT_STATS);
        this.createPNG = controlerConfigGroup.isCreateGraphs();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        this.collectVktStatsInfo(afterMobsimEvent);
    }

    private void collectVktStatsInfo(AfterMobsimEvent event) {
        Map<String, Double> distances = new HashMap<>();
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;
                    String type = null;

                    if (leg.getMode().equals(TransportMode.car)) {
                        type = INDIVIDUAL_TRAVEL;
                    } else if (leg.getMode().equals(Drs.DRIVER_MODE)) {
                        String carpoolingStatus = DrsUtil.getCarpoolingStatus(leg);
                        if (carpoolingStatus == null) {
                            type = INDIVIDUAL_TRAVEL;
                        } else if (carpoolingStatus.equals(Drs.VALUE_STATUS_BEFORE_AFTER)) {
                            type = BEFORE_AFTER_CARPOOLING_TRAVEL;
                        } else if (carpoolingStatus.equals(Drs.VALUE_STATUS_CARPOOLING)) {
                            type = CARPOOLING_TRAVEL;
                        }
                    }

                    if (type != null) {
                        double distance = leg.getRoute().getDistance() / 1000;
                        distances.put(type, distance + distances.getOrDefault(type, 0d));
                    }
                }
            }
        }

        this.iterationHistories.put(event.getIteration(), distances);

        try (BufferedWriter requestOut = IOUtils.getBufferedWriter(this.requestFileName + ".txt")) {
            requestOut.write("Iteration");
            requestOut.write("\t" + CARPOOLING_TRAVEL);
            requestOut.write("\t" + BEFORE_AFTER_CARPOOLING_TRAVEL);
            requestOut.write("\t" + INDIVIDUAL_TRAVEL);
            requestOut.write("\n");

            for (int iteration = 0; iteration <= event.getIteration(); ++iteration) {
                Map<String, Double> distancesAtIteration = this.iterationHistories.get(iteration);
                requestOut.write(String.valueOf(iteration));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(CARPOOLING_TRAVEL, 0d));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(BEFORE_AFTER_CARPOOLING_TRAVEL, 0d));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(INDIVIDUAL_TRAVEL, 0d));
                requestOut.write("\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (this.createPNG) {
            String[] categories = iterationHistories.keySet().stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
            StackedBarChart chart = new StackedBarChart("Motorized Private Vehicles Kilometers Traveled Statistics",
                    "iteration",
                    "Kilometer", categories);

            double[] carpoolValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(CARPOOLING_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(CARPOOLING_TRAVEL, carpoolValues);

            double[] beforeAfterCarpoolValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(BEFORE_AFTER_CARPOOLING_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(BEFORE_AFTER_CARPOOLING_TRAVEL, beforeAfterCarpoolValues);

            double[] individualValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(INDIVIDUAL_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(INDIVIDUAL_TRAVEL, individualValues);

            chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(0, Color.GREEN);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(2, Color.RED);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(3, Color.BLUE);
            chart.addMatsimLogo();
            chart.saveAsPng(this.requestFileName + ".png", 800, 600);
        }
    }
}
