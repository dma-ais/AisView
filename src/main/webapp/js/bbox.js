/**
* script controlling bounding box 
* inspiration from:
* harrywood.co.uk/maps/examples/openlayers/bbox-selector.html
*/  

var vectors;
var box;
var transform;
	

function endDrag(bbox) {
	var bounds = bbox.getBounds();
	setBounds(bounds);
	drawBox(bounds);
	box.deactivate();
	
	document.getElementById("bbox_drag_instruction").style.display = 'none';
	document.getElementById("bbox_adjust_instruction").style.display = 'block';        
}

function dragNewBox() {
	box.activate();
	transform.deactivate(); //The remove the box with handles
	vectors.destroyFeatures();
	
	document.getElementById("bbox_drag_instruction").style.display = 'block';
	document.getElementById("bbox_adjust_instruction").style.display = 'none';
	
	setBounds(null); 
}

function boxResize(event) {
	setBounds(event.feature.geometry.bounds);
}

function drawBox(bounds) {
	var feature = new OpenLayers.Feature.Vector(bounds.toGeometry());

	vectors.addFeatures(feature);
	transform.setFeature(feature);
}

function toPrecision(zoom, value) {
	var decimals = Math.pow(10, Math.floor(zoom/3));
	return Math.round(value * decimals) / decimals;
}

function setBounds(bounds) {
	if (bounds == null) {
		document.getElementById("bbox_result").innerHTML = "";
		
	} else {
		b = bounds.clone().transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"))
		minlon = toPrecision(map.getZoom(), b.left);
		minlat = toPrecision(map.getZoom(), b.bottom);    
		maxlon = toPrecision(map.getZoom(), b.right);
		maxlat = toPrecision(map.getZoom(), b.top);  
					 
		document.getElementById("bbox_result").innerHTML =
										"minlon=" + minlon + ", " +
										"minlat=" + minlat + ", " +
										"maxlon=" + maxlon + ", " +
										"maxlat=" + maxlat;  
	}
}

function init() {
	map = new OpenLayers.Map("mapdiv");
	var openstreetmap = new OpenLayers.Layer.OSM();
	map.addLayer(openstreetmap);


	var lonlat = new OpenLayers.LonLat(10.830917,55.906094	).transform(
			new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
			new OpenLayers.Projection("EPSG:900913") // to Spherical Mercator
		);

	var zoom = 5;


	vectors = new OpenLayers.Layer.Vector("Vector Layer", {
		displayInLayerSwitcher: false
	});
	map.addLayer(vectors);

	box = new OpenLayers.Control.DrawFeature(vectors, OpenLayers.Handler.RegularPolygon, {
		handlerOptions: {
			sides: 4,
			snapAngle: 90,
			irregular: true,
			persist: true
		}
	});
	box.handler.callbacks.done = endDrag;
	map.addControl(box);

	transform = new OpenLayers.Control.TransformFeature(vectors, {
		rotate: false,
		irregular: true
	});
	transform.events.register("transformcomplete", transform, boxResize);
	map.addControl(transform);
	
	map.addControl(box);
	
	box.activate();
	
	map.setCenter(lonlat, zoom);
	
}
      