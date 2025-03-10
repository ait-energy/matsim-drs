package at.ac.ait.matsim.drs.run;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;

import com.google.common.base.Verify;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class DrsConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "drs";

    public enum OperationalScheme {
        door2door, drive2meetingPoint
    };

    public DrsConfigGroup() {
        super(GROUP_NAME);
    }

    @Parameter
    @Comment("Basic operational scheme of dynamic ride sharing")
    public OperationalScheme operationalScheme = OperationalScheme.door2door;

    @Parameter
    @Comment("Maximum euclidean distance between requests to be considered by the matching algorithm, "
            + "i.e. if both the distance between two requests' origin and destination location are closer than the given value they are potential matches. "
            + "The default value is good for urban areas. "
            + "For large areas with sparsely distributed population and low drs share, you may consider using a larger value.")
    @PositiveOrZero
    public int maxMatchingDistanceMeters = 2000;

    @Parameter
    @Comment("The duration of the time segments used in time segment registers of riders requests. "
            + "Must be larger than riderDepartureTimeAdjustmentSeconds. "
            + "To avoid scenarios where a driver and a rider departure time are close, but cross a segment boundary, "
            + "candidate requests are taken not only from the current segment but also from the one before and after.")
    @Positive
    public int timeSegmentLengthSeconds = 2 * 60 * 60;

    @Parameter
    @Comment("Limits the number of possible riders requests considered for a driver during the matching process. "
            + "Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results. "
            + "To turn off this feature specify a sufficiently big number (not recommended). (inspired by taxi contrib)")
    @Positive
    public int maxPossibleCandidates = 30;

    @Parameter
    @Comment("The amount of time the riders are willing to adjust their departure times. "
            + "During the matching process, the arrival of driver to pick-up point is checked whether it is within "
            + "the rider departure time +- the riderDepartureTimeAdjustment.")
    @PositiveOrZero
    public int riderDepartureTimeAdjustmentSeconds = 15 * 60;

    @Parameter
    @Comment("The amount of time the driver is expected to wait until the rider enters the vehicle.")
    @PositiveOrZero
    public int pickupWaitingSeconds = 0;

    @Parameter
    @Comment("The amount of money per kilometre the driver gains for a rider (typically positive)")
    public double driverProfitPerKm = 0;

    @Parameter
    @Comment("Daily price for car usage including when using the private car as drsDriver. "
            + "If specified here do not additionaly specify it in planCalcScore.scoringParameters.modeParams.dailyMonetaryConstant - "
            + "otherwise it will be counted twice (typically negative)")
    public double carAndDrsDailyMonetaryConstant = 0;

    @Parameter
    @Comment("Minimum length of legs (routed with the drsDriver mode) to be considered for the drsDriver mode. 0 means no minimum.")
    @PositiveOrZero
    public int minDriverLegMeters = 10;

    @Parameter
    @Comment("Minimum length of legs (routed with the drsDriver mode) to be considered for the drsRider mode. 0 means no minimum.")
    @PositiveOrZero
    public int minRiderLegMeters = 10;

    // The DMC modules allows for a lot of extensions,
    // but it was not possible to introduce something like
    // <parameterset type="modeAvailability:CarAndDrsDriver">
    // -> This means we have to configure the modes in our module config
    @Parameter("DiscreteModeChoice.modeAvailability.CarAndDrsDriver.availableModes")
    @Comment("Available modes to be used when using DMC with modeAvailability=CarAndDrsDriver. Comma-separated list of modes.")
    public String dmcCarAndDrsDriverAvailableModes = "car,drsDriver,drsRider";

    public List<String> getDmcCarAndDrsDriverAvailableModes() {
        if (dmcCarAndDrsDriverAvailableModes == null) {
            return List.of();
        }
        return Arrays.asList(dmcCarAndDrsDriverAvailableModes.split(",")).stream().map(String::trim)
                .collect(Collectors.toList());
    }

    @Parameter
    @Comment("Geopackage with a layer 'meeting_points' or Shapefile containing point geometries "
            + " with the optional attributes 'id (string)' and an 'linkId' (string). " +
            "Points without linkId (or an invalid one) are assigned to the nearset link.")
    @Nullable
    public String meetingPointsGeoFile;

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);

        if (operationalScheme.equals(OperationalScheme.drive2meetingPoint)) {
            Verify.verify(meetingPointsGeoFile != null,
                    "meetingPointsGeoFile is required when using " + operationalScheme);
            Verify.verify(Files.exists(Paths.get(meetingPointsGeoFile)),
                    "the specified meetingPointsGeoFile '" + meetingPointsGeoFile + "'' does not exist");
        }
    }

}
