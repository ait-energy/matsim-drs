package at.ac.ait.matsim.domino.carpooling.analysis;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CarpoolTripsInfoCollector {
    public static final String FILENAME_CARPOOLTRIPS = "carpool_trips";
    private final String tripsFileName;
    public CarpoolTripsInfoCollector(OutputDirectoryHierarchy controlerIO) {
        this.tripsFileName = controlerIO.getOutputFilename(FILENAME_CARPOOLTRIPS);
    }

    public void printInfoToCsv(HashMap<CarpoolingRequest, CarpoolingRequest> matchMap){
        BufferedWriter writer = IOUtils.getBufferedWriter(this.tripsFileName + ".csv");
        try {
            writer.write("driver,rider,driver trip purpose,rider trip purpose,driver originX,driver originY," +
                    "driver destinationX,driver destinationY,rider originX,rider originY,rider destinationX," +
                    "rider destinationY,driver departure time,rider departure time");
            for (Map.Entry<CarpoolingRequest, CarpoolingRequest> entry : matchMap.entrySet()) {
                Person driver = entry.getKey().getPerson();
                Person rider = entry.getValue().getPerson();
                Activity driverStartAct = entry.getKey().getTrip().getOriginActivity();
                Activity driverEndAct = entry.getKey().getTrip().getDestinationActivity();
                Activity riderStartAct = entry.getValue().getTrip().getOriginActivity();
                Activity riderEndAct = entry.getValue().getTrip().getDestinationActivity();

                writer.write("\n"+driver.getId()+","+rider.getId()+","+driverEndAct.getType()+","+riderEndAct.getType()
                +","+driverStartAct.getCoord().getX()+","+driverStartAct.getCoord().getY()+","+driverEndAct.getCoord().getX()
                +","+driverEndAct.getCoord().getY()+","+riderStartAct.getCoord().getX()+","+riderStartAct.getCoord().getY()
                +","+riderEndAct.getCoord().getX()+","+riderEndAct.getCoord().getY()+","+entry.getKey().getDepartureTime()
                +","+entry.getValue().getDepartureTime());
            }
            writer.flush();
            writer.close();
        } catch (IOException var1) {
            var1.printStackTrace();
            throw new UncheckedIOException(var1);
        }

    }

}
