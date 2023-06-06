clock = via.getOverlay("Clock")
clock.visible = true
//clock.fgColor = 0xFFFF0000 // red
//clock.bgColor = 0x7FFFFF00 // yellow, semi transparent
//clock.showAmPm = false
clock.analog = false // switch to HH:MM:SS display

northArrow = via.getOverlay("North Arrow")
northArrow.visible = false
//northArrow.fgColor = 0xFF00FF00 // green
//northArrow.bgColor = 0xFF0000FF // blue
//northArrow.size = 50
//northArrow.angle = 45 / 180 * Math.PI // rotate map

scaleBar = via.getOverlay("Scale Bar")
scaleBar.visible = true

// create background layer
osmLayer = via.createLayer("webmap")
osmLayer.name = "OSM"
osmLayer.crs = "EPSG:31256";
// show the list of available styles
//print(osmLayer.getAvailableStyles());
osmLayer.style = "osm";
//osmLayer.style = "http://c.tiles.wmflabs.org/hillshading/{z}/{x}/{y}.png";
// set transparency: 0 = transparent, 100 = opaque
osmLayer.opacity = 100;
osmLayer.visible = false;

network = via.createDataset("/tmp/matsim/output_network.xml.gz");
events = via.createDataset("/tmp/matsim/output_events.xml.gz");

// create a layer of type "network" with the specified network dataset
networkLayer = via.createLayer("network", network);

// layers that use events must explicitly load those events using "loadEvents()"
vehiclesLayer = via.createLayer("vehicles", network, events);
vehiclesLayer.loadEvents();

agentsLayer = via.createLayer("agents", network, events);
agentsLayer.loadEvents();

//via.metadata().importAll("/tmp/matsim/via_metadata.xml");
//networkLayer.loadStyle("Current Styles");
