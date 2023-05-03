package at.ac.ait.matsim.domino.carpooling.engine;

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

import com.google.inject.Inject;

import at.ac.ait.matsim.domino.carpooling.events.CarpoolingPickupEvent;
import at.ac.ait.matsim.domino.carpooling.events.CarpoolingPickupEventHandler;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;

public class CarpoolingSimulationStats
        implements CarpoolingPickupEventHandler, VehicleAbortsEventHandler, PersonStuckEventHandler,
        AfterMobsimListener {

    private static final String DEBUG_FILE = "debug_events.txt";
    private static final String CSV_FILE = "carpooling_sim_stats.csv";
    private int currentIteration, successfulPickups, stuckRiders;
    private OutputDirectoryHierarchy outputHierarchy;
    private BufferedWriter debugWriter;

    @Inject
    public CarpoolingSimulationStats(OutputDirectoryHierarchy outputHierarchy) {
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
        debugWriter = createWriter(iteration, DEBUG_FILE);
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
        String msg = String.format("%s @ %d: veh: %s, link: %s",
                event.getEventType(),
                (int) event.getTime(),
                event.getVehicleId(),
                event.getLinkId());
        writeMessageToDebugFileWithNewline(msg);
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        if (event.getLegMode().equals(Carpooling.RIDER_MODE)) {
            stuckRiders++;
        }
        String msg = String.format("%s @ %d: pers: %s, link: %s, legMode: %s",
                event.getEventType(),
                (int) event.getTime(),
                event.getPersonId(),
                event.getLinkId(),
                event.getLegMode());
        writeMessageToDebugFileWithNewline(msg);
    }

    @Override
    public void handleEvent(CarpoolingPickupEvent event) {
        successfulPickups++;
        String msg = String.format("%s @ %d: rider: %s, driver: %s, link: %s, vehicle: %s",
                event.getEventType(),
                (int) event.getTime(),
                event.getRiderId(),
                event.getDriverId(),
                event.getLinkId(),
                event.getVehicleId());
        writeMessageToDebugFileWithNewline(msg);
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
