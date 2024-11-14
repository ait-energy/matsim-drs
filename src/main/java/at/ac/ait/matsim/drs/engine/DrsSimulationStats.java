package at.ac.ait.matsim.drs.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.events.DrsPickupEvent;
import at.ac.ait.matsim.drs.events.DrsPickupEventHandler;
import at.ac.ait.matsim.drs.run.Drs;

public class DrsSimulationStats implements DrsPickupEventHandler, VehicleAbortsEventHandler, PersonStuckEventHandler,
        StartupListener, AfterMobsimListener {
    private static final boolean DEBUG = false;
    private static final String FILENAME_SIM_STATS = "drs_sim_stats";
    private static final String FILENAME_DEBUG_EVENTS = "drs_debug_events.txt";
    private final String simStatsFileName;
    private final String delimiter;

    private int successfulPickups, stuckRiders;
    private OutputDirectoryHierarchy outputHierarchy;
    private BufferedWriter debugWriter;

    @Inject
    public DrsSimulationStats(GlobalConfigGroup globalConfig, OutputDirectoryHierarchy outputHierarchy) {
        this.simStatsFileName = outputHierarchy.getOutputFilename(FILENAME_SIM_STATS);
        this.delimiter = globalConfig.getDefaultDelimiter();
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void reset(int iteration) {
        successfulPickups = 0;
        stuckRiders = 0;

        if (debugWriter != null) {
            try {
                debugWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG) {
            debugWriter = createWriter(iteration, FILENAME_DEBUG_EVENTS);
        }
    }

    private BufferedWriter createWriter(int iteration, String fileName) {
        Path path = Paths.get(outputHierarchy.getIterationPath(iteration));
        try {
            BufferedWriter writer = Files.newBufferedWriter(path.resolve(fileName));
            writer.flush();
            return writer;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void handleEvent(VehicleAbortsEvent event) {
        if (!DEBUG) {
            return;
        }

        String msg = String.format("%s %s: veh: %s, link: %s",
                Time.writeTime(event.getTime()),
                event.getEventType(),
                event.getVehicleId(),
                event.getLinkId());
        writeMessageToDebugFileWithNewline(msg);
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        if (event.getLegMode() != null && event.getLegMode().equals(Drs.RIDER_MODE)) {
            stuckRiders++;
        }

        if (DEBUG) {
            String msg = String.format("%s %s: pers: %s, link: %s, legMode: %s",
                    Time.writeTime(event.getTime()),
                    event.getEventType(),
                    event.getPersonId(),
                    event.getLinkId(),
                    event.getLegMode());
            writeMessageToDebugFileWithNewline(msg);
        }
    }

    @Override
    public void handleEvent(DrsPickupEvent event) {
        successfulPickups++;

        if (DEBUG) {
            String msg = String.format("%s %s: rider: %s, driver: %s, link: %s, vehicle: %s",
                    Time.writeTime(event.getTime()),
                    event.getEventType(),
                    event.getRiderId(),
                    event.getDriverId(),
                    event.getLinkId(),
                    event.getVehicleId());
            writeMessageToDebugFileWithNewline(msg);
        }
    }

    private void writeMessageToDebugFileWithNewline(String msg) {
        if (debugWriter != null) {
            try {
                debugWriter.write(msg);
                debugWriter.write("\n");
                debugWriter.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.err.println(msg);
        }
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(this.simStatsFileName + ".csv")) {
            writer.write("Iteration" + delimiter + "successfulPickups" + delimiter + "stuckRiders\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(this.simStatsFileName + ".csv")) {
            writer.write(event.getIteration() + delimiter + successfulPickups + delimiter + stuckRiders + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
