package at.ac.ait.matsim.drs.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.systems.grid.h3.H3Utils;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;

import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;

public class H3Experiments {
    public static void main(String[] args) {
        playground();
        writeCellsForTestNetwork();
    }

    public static void playground() {
        H3Core h3core = H3Utils.getInstance();
        String viennaAddress = H3Utils.getH3Address(new Coord(16.37, 48.21), 8);
        System.out.println("vienna: " + viennaAddress);
        System.out.println("vienna disk: " + h3core.gridDisk(viennaAddress, 1));
        for (int res = 0; res < 15; res++) {
            String address = h3core.latLngToCellAddress(48.21, 16.37, res);
            List<String> gridDisk = h3core.gridDisk(address, 1);
            String neighbour = gridDisk.getFirst();
            for (int i = 1; i < gridDisk.size() && address.equals(neighbour); i++) {
                neighbour = gridDisk.get(i);
            }
            System.out.println("resolution " + res + ": " + address + "-> " + neighbour);
            String directedEdge = h3core.cellsToDirectedEdge(address, neighbour);
            System.out.println(directedEdge);
            System.out.println(h3core.getHexagonEdgeLengthAvg(res, LengthUnit.m) + "m avg vs. "
                    + h3core.edgeLength(directedEdge, LengthUnit.m) + "m actually");
        }
    }

    public static void writeCellsForTestNetwork() {
        String crsString = "EPSG:31256";
        CoordinateReferenceSystem crs = MGC.getCRS(crsString);
        Network network = NetworkUtils.readNetwork("data/floridsdorf/network.xml");

        for (int resolution = 4; resolution <= 10; resolution++) {
            H3ZoneSystem h3 = new H3ZoneSystem(crsString, resolution, network, z -> true);
            PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("id", String.class)
                    .setCrs(crs).setName("zones_" + resolution).create();
            Collection<SimpleFeature> features = new ArrayList<>();

            for (Entry<Id<Zone>, Zone> entry : h3.getZones().entrySet()) {
                long id = Long.parseLong(entry.getKey().toString());
                String address = Long.toHexString(id);
                features.add(
                        factory.createPolygon(entry.getValue().getPreparedGeometry().getGeometry().getCoordinates(),
                                new Object[] { address }, address));
            }
            GeoFileWriter.writeGeometries(features, "/tmp/floridsdorf-h3grid.gpkg");
        }
    }

}
