package at.ac.ait.matsim.domino.carpooling.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingMatch;
import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;

public class CarpoolTripsInfoCollector {
    public static final String FILENAME_MATCHED_CARPOOLING_TRIPS = "carpooling_matched_trips";
    private final String matchedTripsFileName;
    public static final String FILENAME_UNMATCHED_CARPOOLING_TRIPS = "carpooling_unmatched_trips";
    private final String unmatchedTripsFileName;

    public CarpoolTripsInfoCollector(OutputDirectoryHierarchy controlerIO) {
        this.matchedTripsFileName = controlerIO.getOutputFilename(FILENAME_MATCHED_CARPOOLING_TRIPS);
        this.unmatchedTripsFileName = controlerIO.getOutputFilename(FILENAME_UNMATCHED_CARPOOLING_TRIPS);
    }

    public void printMatchedRequestsToCsv(List<CarpoolingMatch> matches) {
        BufferedWriter writer = IOUtils.getBufferedWriter(this.matchedTripsFileName + ".csv");
        try {
            writer.write(
                    "driver,rider,driver trip origin,driver trip purpose,rider trip origin,rider trip purpose,driver originX,driver originY,"
                            +
                            "driver destinationX,driver destinationY,rider originX,rider originY,rider destinationX," +
                            "rider destinationY,driver departure time,rider departure time,detour factor");
            for (CarpoolingMatch match : matches) {
                Person driver = match.getDriver().getPerson();
                Person rider = match.getRider().getPerson();
                Activity driverStartAct = match.getDriver().getTrip().getOriginActivity();
                Activity driverEndAct = match.getDriver().getTrip().getDestinationActivity();
                Activity riderStartAct = match.getRider().getTrip().getOriginActivity();
                Activity riderEndAct = match.getRider().getTrip().getDestinationActivity();

                writer.write("\n" + driver.getId() + "," + rider.getId() + "," + driverStartAct.getType() + "," +
                        driverEndAct.getType() + "," + riderStartAct.getType() + "," + riderEndAct.getType() + "," +
                        driverStartAct.getCoord().getX() + "," + driverStartAct.getCoord().getY() + "," +
                        driverEndAct.getCoord().getX() + "," + driverEndAct.getCoord().getY() + ","
                        + riderStartAct.getCoord().getX() +
                        "," + riderStartAct.getCoord().getY() + "," + riderEndAct.getCoord().getX() + ","
                        + riderEndAct.getCoord().getY()
                        + "," + match.getDriver().getDepartureTime() + "," + match.getRider().getDepartureTime() + ","
                        + match.getDetourFactor());
            }
            writer.flush();
            writer.close();
        } catch (IOException var1) {
            var1.printStackTrace();
            throw new UncheckedIOException(var1);
        }
    }

    public void printUnMatchedRequestsToCsv(List<CarpoolingRequest> unmatchedDriverRequests,
            List<CarpoolingRequest> unmatchedRiderRequests) {
        BufferedWriter writer = IOUtils.getBufferedWriter(this.unmatchedTripsFileName + ".csv");
        try {
            writer.write("person,mode,trip origin,trip purpose,originX,originY," +
                    "destinationX,destinationY,departure time");
            for (CarpoolingRequest unmatchedDriverRequest : unmatchedDriverRequests) {
                Person driver = unmatchedDriverRequest.getPerson();
                Activity driverStartAct = unmatchedDriverRequest.getTrip().getOriginActivity();
                Activity driverEndAct = unmatchedDriverRequest.getTrip().getDestinationActivity();

                writer.write(
                        "\n" + driver.getId() + "," + Carpooling.DRIVER_MODE + "," + driverStartAct.getType() + "," +
                                driverEndAct.getType() + "," + driverStartAct.getCoord().getX() + ","
                                + driverStartAct.getCoord().getY()
                                + "," + driverEndAct.getCoord().getX() + "," + driverEndAct.getCoord().getY() + "," +
                                unmatchedDriverRequest.getDepartureTime());
            }
            for (CarpoolingRequest unmatchedRiderRequest : unmatchedRiderRequests) {
                Person rider = unmatchedRiderRequest.getPerson();
                Activity riderStartAct = unmatchedRiderRequest.getTrip().getOriginActivity();
                Activity riderEndAct = unmatchedRiderRequest.getTrip().getDestinationActivity();

                writer.write("\n" + rider.getId() + "," + Carpooling.RIDER_MODE + "," + riderStartAct.getType() + "," +
                        riderEndAct.getType() + "," + riderStartAct.getCoord().getX() + ","
                        + riderStartAct.getCoord().getY()
                        + "," + riderEndAct.getCoord().getX() + "," + riderEndAct.getCoord().getY() + "," +
                        unmatchedRiderRequest.getDepartureTime());
            }
            writer.flush();
            writer.close();
        } catch (IOException var1) {
            var1.printStackTrace();
            throw new UncheckedIOException(var1);
        }

    }
}
