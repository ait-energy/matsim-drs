package at.ac.ait.matsim.domino.carpooling.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

import at.ac.ait.matsim.domino.carpooling.request.CarpoolingRequest;
import at.ac.ait.matsim.domino.carpooling.run.Carpooling;

public class StatsCollector {

    public static BufferedWriter createWriter(String path, String header) {
        BufferedWriter writer = IOUtils.getBufferedWriter(path);
        try {
            writer.write(header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer;
    }

    public static void collectDriversRequestsStats(BufferedWriter writer, CarpoolingRequest driverRequest,
            List<CarpoolingRequest> nearestRequests, List<CarpoolingRequest> filteredRidersRequests,
            CarpoolingRequest bestRiderRequest) {
        try {
            if (bestRiderRequest == null) {
                writer.write("\n" + driverRequest.getId() + "," + driverRequest.getPerson().getId() + ","
                        + driverRequest.getDepartureTime() + ","
                        + driverRequest.getFromLink().getFromNode().getCoord().getX() + ","
                        + driverRequest.getFromLink().getFromNode().getCoord().getY() + ","
                        + driverRequest.getToLink().getFromNode().getCoord().getX() + ","
                        + driverRequest.getToLink().getFromNode().getCoord().getY() + "," + "False,"
                        + nearestRequests.size() + "," + filteredRidersRequests.size());
            } else {
                writer.write("\n" + driverRequest.getId() + "," + driverRequest.getPerson().getId() + ","
                        + driverRequest.getDepartureTime() + ","
                        + driverRequest.getFromLink().getFromNode().getCoord().getX() + ","
                        + driverRequest.getFromLink().getFromNode().getCoord().getY() + ","
                        + driverRequest.getToLink().getFromNode().getCoord().getX() + ","
                        + driverRequest.getToLink().getFromNode().getCoord().getY() + "," + "True,"
                        + nearestRequests.size() + "," + filteredRidersRequests.size());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void close(BufferedWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void collectRidersRequestsStats(BufferedWriter writer, List<CarpoolingRequest> ridersRequests) {
        try {
            for (CarpoolingRequest request : ridersRequests) {
                if (request.getMode().equals(Carpooling.RIDER_MODE)) {
                    if (request.isMatched()) {
                        writer.write("\n" + request.getId() + "," + request.getPerson().getId() + ","
                                + request.getDepartureTime() + ","
                                + request.getFromLink().getFromNode().getCoord().getX() + ","
                                + request.getFromLink().getFromNode().getCoord().getY() + ","
                                + request.getToLink().getFromNode().getCoord().getX() + ","
                                + request.getToLink().getFromNode().getCoord().getY() + ",True");
                    } else {
                        writer.write("\n" + request.getId() + "," + request.getPerson().getId() + ","
                                + request.getDepartureTime() + ","
                                + request.getFromLink().getFromNode().getCoord().getX() + ","
                                + request.getFromLink().getFromNode().getCoord().getY() + ","
                                + request.getToLink().getFromNode().getCoord().getX() + ","
                                + request.getToLink().getFromNode().getCoord().getY() + ",False");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
