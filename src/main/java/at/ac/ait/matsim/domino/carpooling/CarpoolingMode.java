package at.ac.ait.matsim.domino.carpooling;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CarpoolingMode {
    public static final String DRIVER = "carpoolingDriver";
    public static final String PASSENGER = "carpoolingPassenger";

    public static final List<String> ALL = ImmutableList.of(DRIVER, PASSENGER);
}
