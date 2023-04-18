package at.ac.ait.matsim.domino.carpooling.analysis;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;

public class VktStatsControlerListener implements AfterMobsimListener {
    public static final String FILENAME_VKT_STATS = "carpooling_vkt_stats";
    public static final String CARPOOL_TRAVEL = "carpool travel";
    public static final String INDIVIDUAL_TRAVEL = "individual travel";
    public static final String BEFORE_AFTER_CARPOOL_TRAVEL = "before and after carpool";
    private final Population population;
    private final String requestFileName;
    private final boolean createPNG;
    Map<Integer, Map<String, Double>> iterationHistories = new HashMap<>();
    private final Map<String, Double> totalDistance = new HashMap<>();

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
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Leg) {
                    if (Objects.equals(((Leg) planElement).getMode(), TransportMode.car)) {
                        if (totalDistance.get(INDIVIDUAL_TRAVEL) == null) {
                            this.totalDistance.put(INDIVIDUAL_TRAVEL,
                                    ((Leg) planElement).getRoute().getDistance() / 1000);
                        } else {
                            this.totalDistance.put(INDIVIDUAL_TRAVEL, totalDistance.get(INDIVIDUAL_TRAVEL)
                                    + ((Leg) planElement).getRoute().getDistance() / 1000);
                        }
                    } else if (Objects.equals(((Leg) planElement).getMode(), Carpooling.RIDER_MODE)) {
                        if (CarpoolingUtil.getCarpoolingStatus((Leg) planElement) == null) {
                            if (totalDistance.get(INDIVIDUAL_TRAVEL) == null) {
                                this.totalDistance.put(INDIVIDUAL_TRAVEL,
                                        ((Leg) planElement).getRoute().getDistance() / 1000);
                            } else {
                                this.totalDistance.put(INDIVIDUAL_TRAVEL, totalDistance.get(INDIVIDUAL_TRAVEL)
                                        + ((Leg) planElement).getRoute().getDistance() / 1000);
                            }
                        }
                    } else if (Objects.equals(((Leg) planElement).getMode(), Carpooling.DRIVER_MODE)) {
                        if (CarpoolingUtil.getCarpoolingStatus((Leg) planElement) != null) {
                            if (CarpoolingUtil.getCarpoolingStatus((Leg) planElement).equals("beforeAndAfterCarpool")){
                                if (totalDistance.get(BEFORE_AFTER_CARPOOL_TRAVEL) == null) {
                                    this.totalDistance.put(BEFORE_AFTER_CARPOOL_TRAVEL,
                                            ((Leg) planElement).getRoute().getDistance() / 1000);
                                } else {
                                    this.totalDistance.put(BEFORE_AFTER_CARPOOL_TRAVEL, totalDistance.get(BEFORE_AFTER_CARPOOL_TRAVEL)
                                            + ((Leg) planElement).getRoute().getDistance() / 1000);
                                }
                            } else if (CarpoolingUtil.getCarpoolingStatus((Leg) planElement).equals("carpool")) {
                                if (totalDistance.get(CARPOOL_TRAVEL) == null) {
                                    this.totalDistance.put(CARPOOL_TRAVEL,
                                            ((Leg) planElement).getRoute().getDistance() / 1000);
                                } else {
                                    this.totalDistance.put(CARPOOL_TRAVEL, totalDistance.get(CARPOOL_TRAVEL)
                                            + ((Leg) planElement).getRoute().getDistance() / 1000);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Double> totalDistancesHistory = new HashMap<>();
        if (totalDistance.get(CARPOOL_TRAVEL) == null) {
            totalDistancesHistory.put(CARPOOL_TRAVEL, 0.0);
        } else {
            totalDistancesHistory.put(CARPOOL_TRAVEL, totalDistance.get(CARPOOL_TRAVEL));
        }
        if (totalDistance.get(INDIVIDUAL_TRAVEL) == null) {
            totalDistancesHistory.put(INDIVIDUAL_TRAVEL, 0.0);
        } else {
            totalDistancesHistory.put(INDIVIDUAL_TRAVEL, totalDistance.get(INDIVIDUAL_TRAVEL));
        }
        if (totalDistance.get(BEFORE_AFTER_CARPOOL_TRAVEL) == null) {
            totalDistancesHistory.put(BEFORE_AFTER_CARPOOL_TRAVEL, 0.0);
        } else {
            totalDistancesHistory.put(BEFORE_AFTER_CARPOOL_TRAVEL, totalDistance.get(BEFORE_AFTER_CARPOOL_TRAVEL));
        }
        this.iterationHistories.put(event.getIteration(), totalDistancesHistory);

        BufferedWriter requestOut = IOUtils.getBufferedWriter(this.requestFileName + ".txt");

        try {
            requestOut.write("Iteration");
            requestOut.write("\t" + CARPOOL_TRAVEL);
            requestOut.write("\t" + BEFORE_AFTER_CARPOOL_TRAVEL);
            requestOut.write("\t" + INDIVIDUAL_TRAVEL);
            requestOut.write("\n");

            for (int iteration = 0; iteration <= event.getIteration(); ++iteration) {
                requestOut.write(String.valueOf(iteration));

                Map<String, Double> carpoolMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + carpoolMap.get(CARPOOL_TRAVEL));

                Map<String, Double> beforeAfterCarpoolMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + beforeAfterCarpoolMap.get(BEFORE_AFTER_CARPOOL_TRAVEL));

                Map<String, Double> individualMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + individualMap.get(INDIVIDUAL_TRAVEL));

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
            StackedBarChart chart = new StackedBarChart("Motorized Private Vehicles Kilometers Traveled Statistics", "iteration",
                    "Kilometer", categories);

            double[] carpoolValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(CARPOOL_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(CARPOOL_TRAVEL, carpoolValues);

            double[] beforeAfterCarpoolValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(BEFORE_AFTER_CARPOOL_TRAVEL, 0.0))
                    .toArray();
            chart.addSeries(BEFORE_AFTER_CARPOOL_TRAVEL, beforeAfterCarpoolValues);

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
        this.totalDistance.clear();
    }
}
