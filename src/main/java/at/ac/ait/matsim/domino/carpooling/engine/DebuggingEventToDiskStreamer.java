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

import com.google.inject.Inject;

public class DebuggingEventToDiskStreamer implements VehicleAbortsEventHandler, PersonStuckEventHandler {

    private static final String FILENAME = "debug_events.txt";
    private OutputDirectoryHierarchy outputHierarchy;
    private BufferedWriter writer;

    @Inject
    public DebuggingEventToDiskStreamer(OutputDirectoryHierarchy outputHierarchy) {
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void reset(int iteration) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        createWriter(iteration);
    }

    private void createWriter(int iteration) {
        Path path = Paths.get(outputHierarchy.getIterationPath(iteration));
        try {
            writer = Files.newBufferedWriter(path.resolve(FILENAME));
            writer.write("ahoi iteration " + iteration + "!\n");
            writer.flush();
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
        writeMessageToFileWithNewline(msg);
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        String msg = String.format("%s @ %d: pers: %s, link: %s, legMode: %s",
                event.getEventType(),
                (int) event.getTime(),
                event.getPersonId(),
                event.getLinkId(),
                event.getLegMode());
        writeMessageToFileWithNewline(msg);
    }

    // @Override
    // public void handleEvent(PersonMoneyEvent event) {
    // String msg = String.format("%s @ %d: pers: %s, amount: %.1f",
    // event.getEventType(),
    // (int) event.getTime(),
    // event.getPersonId(),
    // event.getAmount());
    // writeMessageToFileWithNewline(msg);
    // }

    private void writeMessageToFileWithNewline(String msg) {
        if (writer != null) {
            try {
                writer.write(msg);
                writer.write("\n");
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.err.println(msg);
        }
    }

}
