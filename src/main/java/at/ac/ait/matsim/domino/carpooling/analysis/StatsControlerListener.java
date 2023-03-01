package at.ac.ait.matsim.domino.carpooling.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;


public class StatsControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

    public static final String FILENAME_STATS = "stats";

    final private Population population;
    final private BufferedWriter out;
    final private String fileName;
    private final boolean createPNG;
    private final ControlerConfigGroup controlerConfigGroup;
    private int minIteration = 0;


    @Inject
    StatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO) {
        this.controlerConfigGroup = controlerConfigGroup;
        this.population = population1;
        this.fileName = controlerIO.getOutputFilename(FILENAME_STATS);
        this.createPNG = controlerConfigGroup.isCreateGraphs();
        this.out = IOUtils.getBufferedWriter(this.fileName + ".txt");
        try {
            this.out.write("ITERATION\tTEXT\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void notifyStartup(final StartupEvent event) {
        this.minIteration = controlerConfigGroup.getFirstIteration();
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        collectStats(event);
    }

    private void collectStats(final IterationEndsEvent event) {
        for (Person person : this.population.getPersons().values()) {
            try {
                this.out.write(event.getIteration() + "\t" + "TEXT"+ "\n");
                this.out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (this.createPNG && event.getIteration() > this.minIteration) {
            XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
            // chart.addSeries("avg. worst score", this.scoreHistory.get( ScoreItem.worst ) ) ;
            chart.addMatsimLogo();
            chart.saveAsPng(this.fileName + ".png", 800, 600);
        }
    }

    @Override
    public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
        try {
            this.out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
