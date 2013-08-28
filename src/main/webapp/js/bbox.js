/**
* script controlling bounding box 
* inspiration from:
* harrywood.co.uk/maps/examples/openlayers/bbox-selector.html
*/  

var vectors;
var box;
var transform;
var boxDraggedOnce = false;
var boxDrawnOnce = false;

function init() {
    console.log('init()');
    map = new OpenLayers.Map("mapdiv");
    var openstreetmap = new OpenLayers.Layer.OSM();
    map.addLayer(openstreetmap);

    var lonlat = new OpenLayers.LonLat(10.830917,55.906094	).transform(
        new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
        new OpenLayers.Projection("EPSG:900913") // to Spherical Mercator
    );

    var zoom = 2;

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


function endDrag(bbox) {
	var bounds = bbox.getBounds();
	console.log('endDrag() Bounds:'+bounds);
	setBounds(bounds);
	drawBox(bounds);
	box.deactivate();
	
	document.getElementById("bbox_drag_instruction").style.display = 'none';
	document.getElementById("bbox_adjust_instruction").style.display = 'block';

    boxDraggedOnce = true;
}

function newInput(topLeftLat, topLeftLon, buttomRightLat, buttomRightLon) {

    console.log('inputfields: '+topLeftLat+ ',' + topLeftLon + ',' + buttomRightLat +','+ buttomRightLon);

    //fix of collapse of borders of box
    //TODO: take care of 10==10.==10.0...
    if(topLeftLat==buttomRightLat) {
        console.log('same borders');
        var scope = angular.element('#mapInputWarning').scope();
        scope.mapInputWarning = 'Same latitude on both points';
    }
    else if(topLeftLon==buttomRightLon){
        console.log('same borders');
        var scope = angular.element('#mapInputWarning').scope();
        scope.mapInputWarning = 'Same longitude on both points';
    }
    else{

        if (boxDraggedOnce){
            //check if input is blank
            if((topLeftLat!='') && (topLeftLon!='')&&(buttomRightLat!='')&&(buttomRightLon!='')) {
                //same as if dragNewBox();
                box.activate();
                transform.deactivate(); //The remove the box with handles
                vectors.destroyFeatures();

                //same as if endDrag()
                var bounds = new OpenLayers.Bounds();
                bounds.extend(new OpenLayers.LonLat(topLeftLon,topLeftLat));
                bounds.extend(new OpenLayers.LonLat(buttomRightLon,buttomRightLat));

                b = bounds.clone().transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
                console.log('newInput() Bounds:'+b);

                drawBox(b);

                box.deactivate();

                document.getElementById("bbox_drag_instruction").style.display = 'none';
                document.getElementById("bbox_adjust_instruction").style.display = 'block';

                var scope = angular.element('#mapInputWarning').scope();
                scope.mapInputWarning = '';

            }else{
                console.log('some input is blank-do nothing');
                var scope = angular.element('#mapInputWarning').scope();
                scope.mapInputWarning = 'Some input fields are empty';

            }
        }else { //!boxDraggedOnce
            if(!isNaN(topLeftLat)&&!isNaN(topLeftLon)&&!isNaN(buttomRightLat)&&!isNaN(buttomRightLon)){

                if(!boxDrawnOnce) {
                    console.log('getting ready to do some action');

                    //same as if endDrag()
                    var bounds = new OpenLayers.Bounds();
                    bounds.extend(new OpenLayers.LonLat(topLeftLon,topLeftLat));
                    bounds.extend(new OpenLayers.LonLat(buttomRightLon,buttomRightLat));

                    b = bounds.clone().transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
                    console.log('newInput() Bounds:'+b);

                    drawBox(b);

                    box.deactivate();
                    boxDrawnOnce = true;

                    document.getElementById("bbox_drag_instruction").style.display = 'none';
                    document.getElementById("bbox_adjust_instruction").style.display = 'block';

                    var scope = angular.element('#mapInputWarning').scope();
                    scope.mapInputWarning = '';

                }else {     //boxDrawnOnce
                    //check if input is blank
                    if((topLeftLat!='') && (topLeftLon!='')&&(buttomRightLat!='')&&(buttomRightLon!='')) {
                        //same as if dragNewBox();
                        box.activate();
                        transform.deactivate(); //The remove the box with handles
                        vectors.destroyFeatures();

                        //same as if endDrag()
                        var bounds = new OpenLayers.Bounds();
                        bounds.extend(new OpenLayers.LonLat(topLeftLon,topLeftLat));
                        bounds.extend(new OpenLayers.LonLat(buttomRightLon,buttomRightLat));

                        b = bounds.clone().transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
                        console.log('newInput() Bounds:'+b);

                        drawBox(b);

                        box.deactivate();

                        document.getElementById("bbox_drag_instruction").style.display = 'none';
                        document.getElementById("bbox_adjust_instruction").style.display = 'block';

                        var scope = angular.element('#mapInputWarning').scope();
                        scope.mapInputWarning = '';
                    }else{
                        console.log('some input is blank-do nothing');
                        var scope = angular.element('#mapInputWarning').scope();
                        scope.mapInputWarning = 'Some input fields are empty';

                    }
                }
            }else console.log('do nothing yet');
        }
    }
}

function dragNewBox() {
	console.log('dragNewBox()');
    box.activate();
	transform.deactivate(); //The remove the box with handles
	vectors.destroyFeatures();
	
	document.getElementById("bbox_drag_instruction").style.display = 'block';
	document.getElementById("bbox_adjust_instruction").style.display = 'none';
	
	setBounds(null); 
}

function boxResize(event) {
	setBounds(event.feature.geometry.bounds);
	console.log('Bounds boxResize():'+event.feature.geometry.bounds);
}

function drawBox(bounds) {
	console.log('drawBox()');
    var feature = new OpenLayers.Feature.Vector(bounds.toGeometry());

	vectors.addFeatures(feature);
	transform.setFeature(feature);
}

function setBounds(bounds) {
	console.log('setBounds()');
    if (bounds == null) {
		var scope = angular.element('#bbox_result').scope();

        scope.$apply(function(){
                scope.topLeftLat = '';
				scope.topLeftLon = '';
				scope.buttomRightLat = '';
				scope.buttomRightLon = '';
		});
		
		
	} else {
		b = bounds.clone().transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));

		//Sending values to right scope in angular
		var scope = angular.element('#bbox_result_maxLat').scope();
			scope.$apply(function(){
  			scope.topLeftLat = b.top.toFixed(3);
		});
			
		var scope = angular.element('#bbox_result_minLon').scope();
			scope.$apply(function(){
  			scope.topLeftLon = b.left.toFixed(3);
		});
		
		var scope = angular.element('#bbox_result_minLat').scope();
			scope.$apply(function(){
  			scope.buttomRightLat = b.bottom.toFixed(3);
		});
		
		var scope = angular.element('#bbox_result_maxLon').scope();
			scope.$apply(function(){
  			scope.buttomRightLon = b.right.toFixed(3);
		});
	}
}

      