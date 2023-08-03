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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

import at.ac.ait.matsim.drs.events.DrsPickupEvent;
import at.ac.ait.matsim.drs.events.DrsPickupEventHandler;
import at.ac.ait.matsim.drs.run.Drs;

public class DrsSimulationStats
        implements DrsPickupEventHandler, VehicleAbortsEventHandler, PersonStuckEventHandler,
        AfterMobsimListener {

    private static final boolean DEBUG = false;
    private static final String DEBUG_FILE = "debug_events.txt";
    private static final String CSV_FILE = "drs_sim_stats.csv";
    private int currentIteration, successfulPickups, stuckRiders;
    private OutputDirectoryHierarchy outputHierarchy;
    private BufferedWriter debugWriter;

    @Inject
    public DrsSimulationStats(OutputDirectoryHierarchy outputHierarchy) {
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void reset(int iteration) {
        currentIteration = iteration;
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
            debugWriter = createWriter(iteration, DEBUG_FILE);
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
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        try (BufferedWriter writer = createWriter(currentIteration, CSV_FILE)) {
            writer.write("successfulPickups,stuckRiders\n");
            writer.write(successfulPickups + "," + stuckRiders + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
