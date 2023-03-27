package at.ac.ait.matsim.domino.carpooling.analysis;

import at.ac.ait.matsim.domino.carpooling.run.Carpooling;
import at.ac.ait.matsim.domino.carpooling.util.CarpoolingUtil;
import com.google.inject.Inject;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.TransportMode;
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

public class VktStatsControlerListener implements AfterMobsimListener {
    public static final String FILENAME_REQUESTSTATS = "vkt_stats";
    public static final String CARPOOL_TRIP = "carpool trip";
    public static final String INDIVIDUAL_TRIP = "individual trip";
    private final Population population;
    private final String requestFileName;
    private final boolean createPNG;
    Map<Integer, Map<String, Double>> iterationHistories = new HashMap<>();
    private final Map<String, Double> totalDistance = new HashMap<>();

    @Inject
    public VktStatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population, OutputDirectoryHierarchy controlerIO) {
        this.population = population;
        this.requestFileName = controlerIO.getOutputFilename(FILENAME_REQUESTSTATS);
        this.createPNG = controlerConfigGroup.isCreateGraphs();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        this.collectVktStatsInfo(afterMobsimEvent);
    }

    private void collectVktStatsInfo(AfterMobsimEvent event) {
        for (Person person: population.getPersons().values()){
            for (PlanElement planElement :person.getSelectedPlan().getPlanElements()){
                if (planElement instanceof Leg){
                    if (Objects.equals(((Leg) planElement).getMode(), TransportMode.car)){
                        if (totalDistance.get(INDIVIDUAL_TRIP)==null){
                            this.totalDistance.put(INDIVIDUAL_TRIP, ((Leg) planElement).getRoute().getDistance()/1000);
                        }else {
                            this.totalDistance.put(INDIVIDUAL_TRIP, totalDistance.get(INDIVIDUAL_TRIP) + ((Leg) planElement).getRoute().getDistance()/1000);
                        }
                    } else if (Objects.equals(((Leg) planElement).getMode(), Carpooling.DRIVER_MODE)) {
                        if (CarpoolingUtil.getDropoffStatus((Leg) planElement)!=null){
                            if (totalDistance.get(CARPOOL_TRIP)==null){
                                this.totalDistance.put(CARPOOL_TRIP, ((Leg) planElement).getRoute().getDistance()/1000);
                            }else {
                                this.totalDistance.put(CARPOOL_TRIP, totalDistance.get(CARPOOL_TRIP) + ((Leg) planElement).getRoute().getDistance()/1000);
                            }
                            CarpoolingUtil.setDropoffStatus((Leg) planElement,null);
                        }else {
                            if (totalDistance.get(INDIVIDUAL_TRIP)==null){
                                this.totalDistance.put(INDIVIDUAL_TRIP, ((Leg) planElement).getRoute().getDistance()/1000);
                            }else {
                                this.totalDistance.put(INDIVIDUAL_TRIP, totalDistance.get(INDIVIDUAL_TRIP) + ((Leg) planElement).getRoute().getDistance()/1000);
                            }
                        }
                    }
                }
            }
        }

        Map<String, Double> totalDistancesHistory = new HashMap<>();
        if (totalDistance.get(CARPOOL_TRIP)==null){
            totalDistancesHistory.put(CARPOOL_TRIP, 0.0);
        }else{
            totalDistancesHistory.put(CARPOOL_TRIP, totalDistance.get(CARPOOL_TRIP));
        }
        if (totalDistance.get(INDIVIDUAL_TRIP)==null){
            totalDistancesHistory.put(INDIVIDUAL_TRIP, 0.0);
        }else{
            totalDistancesHistory.put(INDIVIDUAL_TRIP, totalDistance.get(INDIVIDUAL_TRIP));
        }
        this.iterationHistories.put(event.getIteration(), totalDistancesHistory);


        BufferedWriter requestOut = IOUtils.getBufferedWriter(this.requestFileName + ".txt");

        try {
            requestOut.write("Iteration");
            requestOut.write("\t" + CARPOOL_TRIP);
            requestOut.write("\t" + INDIVIDUAL_TRIP);
            requestOut.write("\n");

            for(int iteration = 0; iteration <= event.getIteration(); ++iteration) {
                requestOut.write(String.valueOf(iteration));

                Map<String, Double> matchedMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + matchedMap.get(CARPOOL_TRIP));

                Map<String, Double> notMatchedMap = this.iterationHistories.get(iteration);
                requestOut.write("\t" + notMatchedMap.get(INDIVIDUAL_TRIP));

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
            StackedBarChart chart = new StackedBarChart("Vehicle Kilometers Traveled Statistics", "iteration", "Kilometer",categories);

            double[] matchedValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(CARPOOL_TRIP, 0.0))
                    .toArray();
            chart.addSeries(CARPOOL_TRIP,matchedValues);

            double[] notMatchedValues = iterationHistories.values().stream()
                    .mapToDouble(map -> map.getOrDefault(INDIVIDUAL_TRIP, 0.0))
                    .toArray();
            chart.addSeries(INDIVIDUAL_TRIP,notMatchedValues);

            chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(0, Color.GREEN);
            chart.getChart().getCategoryPlot().getRenderer().setSeriesPaint(1, Color.RED);
            chart.addMatsimLogo();
            chart.saveAsPng(this.requestFileName + "_stackedbar.png", 800, 600);
        }
        this.totalDistance.clear();
    }
}
