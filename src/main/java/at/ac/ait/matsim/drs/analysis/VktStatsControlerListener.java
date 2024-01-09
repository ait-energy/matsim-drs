package at.ac.ait.matsim.drs.analysis;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.util.DrsUtil;

public class VktStatsControlerListener implements AfterMobsimListener {
    public static final String FILENAME_VKT_STATS = "drs_vkt_stats";
    public static final String DRS_TRAVEL = "drs travel";
    public static final String INDIVIDUAL_TRAVEL = "individual travel";
    public static final String BEFORE_AFTER_DRS_TRAVEL = "before and after drs";
    private final Population population;
    private final String requestFileName;
    private final ControllerConfigGroup controllerConfigGroup;
    private final Map<Integer, Map<String, Double>> iterationHistories = new HashMap<>();

    @Inject
    public VktStatsControlerListener(ControllerConfigGroup controllerConfigGroup, Population population,
            OutputDirectoryHierarchy controlerIO) {
        this.population = population;
        this.requestFileName = controlerIO.getOutputFilename(FILENAME_VKT_STATS);
        this.controllerConfigGroup = controllerConfigGroup;
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
                        String drsStatus = DrsUtil.getDrsStatus(leg);
                        if (drsStatus == null) {
                            type = INDIVIDUAL_TRAVEL;
                        } else if (drsStatus.equals(Drs.VALUE_STATUS_BEFORE_AFTER)) {
                            type = BEFORE_AFTER_DRS_TRAVEL;
                        } else if (drsStatus.equals(Drs.VALUE_STATUS_DRS)) {
                            type = DRS_TRAVEL;
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
            requestOut.write("\t" + DRS_TRAVEL);
            requestOut.write("\t" + BEFORE_AFTER_DRS_TRAVEL);
            requestOut.write("\t" + INDIVIDUAL_TRAVEL);
            requestOut.write("\n");

            for (int iteration = 0; iteration <= event.getIteration(); ++iteration) {
                Map<String, Double> distancesAtIteration = this.iterationHistories.get(iteration);
                requestOut.write(String.valueOf(iteration));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(DRS_TRAVEL, 0d));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(BEFORE_AFTER_DRS_TRAVEL, 0d));
                requestOut.write("\t" + distancesAtIteration.getOrDefault(INDIVIDUAL_TRAVEL, 0d));
                requestOut.write("\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (DrsUtil.writeGraph(event, controllerConfigGroup)) {
            String[] categories = iterationHistories.keySet().stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
            StackedBarChart chart = new StackedBarChart("Motorized Private Vehicles Kilometers Traveled Statistics",
                    "iteration",
                    "Kilometer", categories);

            double[] drsValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(DRS_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(DRS_TRAVEL, drsValues);

            double[] beforeAfterDrsValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(BEFORE_AFTER_DRS_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(BEFORE_AFTER_DRS_TRAVEL, beforeAfterDrsValues);

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
