package at.ac.ait.matsim.drs.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.h3.H3GridUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class H3Experiments {
    public static void main(String[] args) {
        String crsString = "EPSG:31256";
        CoordinateReferenceSystem crs = MGC.getCRS(crsString);
        Network network = NetworkUtils.readNetwork("/home/mstraub/projects/matsim-drs/data/floridsdorf/network.xml");

        for (int resolution = 4; resolution <= 10; resolution++) {
            Map<String, PreparedGeometry> h3GridFromNetwork = H3GridUtils.createH3GridFromNetwork(network, resolution,
                    crsString);
            PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("id", String.class)
                    .setCrs(crs).setName("zones_" + resolution).create();
            List<SimpleFeature> features = new ArrayList<>();
            for (Entry<String, PreparedGeometry> entry : h3GridFromNetwork.entrySet()) {
                String id = entry.getKey();
                features.add(
                        factory.createPolygon(entry.getValue().getGeometry().getCoordinates(), new Object[] { id },
                                id));
            }
            GeoFileWriter.writeGeometries(features, "/tmp/h3grid.gpkg");
        }
    }

}
