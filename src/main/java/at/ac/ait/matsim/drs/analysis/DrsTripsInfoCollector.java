package at.ac.ait.matsim.drs.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

import at.ac.ait.matsim.drs.request.DrsMatch;
import at.ac.ait.matsim.drs.request.DrsRequest;
import at.ac.ait.matsim.drs.run.Drs;

public class DrsTripsInfoCollector {
    public static final String FILENAME_MATCHED_DRS_TRIPS = "drs_matched_trips";
    private final String matchedTripsFileName;
    public static final String FILENAME_UNMATCHED_DRS_TRIPS = "drs_unmatched_trips";
    private final String unmatchedTripsFileName;

    public DrsTripsInfoCollector(OutputDirectoryHierarchy controlerIO) {
        this.matchedTripsFileName = controlerIO.getOutputFilename(FILENAME_MATCHED_DRS_TRIPS);
        this.unmatchedTripsFileName = controlerIO.getOutputFilename(FILENAME_UNMATCHED_DRS_TRIPS);
    }

    public void printMatchedRequestsToCsv(List<DrsMatch> matches) {
        BufferedWriter writer = IOUtils.getBufferedWriter(this.matchedTripsFileName + ".csv");
        try {
            writer.write(
                    "driver,rider,driver trip origin,driver trip purpose,rider trip origin,rider trip purpose,driver originX,driver originY,"
                            +
                            "driver destinationX,driver destinationY,rider originX,rider originY,rider destinationX," +
                            "rider destinationY,driver departure time,rider departure time,detour factor");
            for (DrsMatch match : matches) {
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

    public void printUnMatchedRequestsToCsv(List<DrsRequest> unmatchedDriverRequests,
            List<DrsRequest> unmatchedRiderRequests) {
        BufferedWriter writer = IOUtils.getBufferedWriter(this.unmatchedTripsFileName + ".csv");
        try {
            writer.write("person,mode,trip origin,trip purpose,originX,originY," +
                    "destinationX,destinationY,departure time");
            for (DrsRequest unmatchedDriverRequest : unmatchedDriverRequests) {
                Person driver = unmatchedDriverRequest.getPerson();
                Activity driverStartAct = unmatchedDriverRequest.getTrip().getOriginActivity();
                Activity driverEndAct = unmatchedDriverRequest.getTrip().getDestinationActivity();

                writer.write(
                        "\n" + driver.getId() + "," + Drs.DRIVER_MODE + "," + driverStartAct.getType() + "," +
                                driverEndAct.getType() + "," + driverStartAct.getCoord().getX() + ","
                                + driverStartAct.getCoord().getY()
                                + "," + driverEndAct.getCoord().getX() + "," + driverEndAct.getCoord().getY() + "," +
                                unmatchedDriverRequest.getDepartureTime());
            }
            for (DrsRequest unmatchedRiderRequest : unmatchedRiderRequests) {
                Person rider = unmatchedRiderRequest.getPerson();
                Activity riderStartAct = unmatchedRiderRequest.getTrip().getOriginActivity();
                Activity riderEndAct = unmatchedRiderRequest.getTrip().getDestinationActivity();

                writer.write("\n" + rider.getId() + "," + Drs.RIDER_MODE + "," + riderStartAct.getType() + "," +
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
