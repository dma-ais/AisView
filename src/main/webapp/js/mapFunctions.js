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
   map = new OpenLayers.Map('mapdiv', {
        controls: [
            new OpenLayers.Control.Navigation(),
            new OpenLayers.Control.PanZoomBar(),
            new OpenLayers.Control.ScaleLine(),
            new OpenLayers.Control.MousePosition({"numDigits": 3,displayProjection: new OpenLayers.Projection("EPSG:4326")}),
            new OpenLayers.Control.KeyboardDefaults()
        ],
        numZoomLevels: 4
    });

    var mapserv = new OpenLayers.Layer.MapServer( "OpenLayers Basic",
        "http://vmap0.tiles.osgeo.org/wms/vmap0",
        {layers: 'basic'},
        {wrapDateLine: true} );


    var openstreetmap = new OpenLayers.Layer.OSM();
    map.addLayer(openstreetmap);

    var lonlat = new OpenLayers.LonLat(0, 50).transform(
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

    bounds.transform(
        new OpenLayers.Projection("EPSG:900913"),   // transform from WGS 1984
        new OpenLayers.Projection("EPSG:4326")      // to Spherical Mercator
    );

    bounds.transform(
        new OpenLayers.Projection("EPSG:4326"),     //  transform from Spherical Mercator
        new OpenLayers.Projection("EPSG:900913")    //  to WGS 1984
    );

    setBounds(bounds);
    drawBox(bounds);
    box.deactivate();

    document.getElementById("bbox_adjust_instruction").style.display = 'block';

    boxDraggedOnce = true;
}

function newInput(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon) {

    if(!isNaN(topLeftLat)&&!isNaN(topLeftLon)&&!isNaN(bottomRightLat)&&!isNaN(bottomRightLon)   //no NaN inputs
        && topLeftLat!='' && topLeftLon!='' && bottomRightLat!='' && bottomRightLon!=''         //no empty strings
        && topLeftLat<90 && topLeftLat>-90 && bottomRightLat<90 && bottomRightLat>-90           //lat control
        && topLeftLon<180 && topLeftLon>-180 && bottomRightLon<180 && bottomRightLon>-180       //lon control
        && topLeftLat!=bottomRightLat && topLeftLon!=bottomRightLon                             //not same values
        ){

        if(!boxDrawnOnce) {

            //same as if endDrag()
            var latLonArray = new Array();
            latLonArray = handleDateLine(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon);

            var bounds = new OpenLayers.Bounds();
            bounds.extend(new OpenLayers.LonLat(latLonArray[1],latLonArray[0]));
            bounds.extend(new OpenLayers.LonLat(latLonArray[3],latLonArray[2]));

            b = bounds.clone().transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());

            drawBox(b);

            box.deactivate();
            boxDrawnOnce = true;

            //document.getElementById("bbox_drag_instruction").style.display = 'none';
            document.getElementById("bbox_adjust_instruction").style.display = 'block';

        }else { //boxDrawnOnce

            //same as if dragNewBox();
            box.activate();
            transform.deactivate(); //The remove the box with handles
            vectors.destroyFeatures();

            //same as if endDrag()
            var latLonArray = new Array();
            latLonArray = handleDateLine(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon);

            var bounds = new OpenLayers.Bounds();
            bounds.extend(new OpenLayers.LonLat(latLonArray[1],latLonArray[0]));
            bounds.extend(new OpenLayers.LonLat(latLonArray[3],latLonArray[2]));

            b = bounds.clone().transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());

            drawBox(b);
            box.deactivate();

            //document.getElementById("bbox_drag_instruction").style.display = 'none';
            document.getElementById("bbox_adjust_instruction").style.display = 'block';
        }
    }//else console.log('do nothing yet - big control');
}

function dragNewBox() {
    box.activate();

    //We need to destroy the transform-feature 'transform' and make it again
    //to deal with collapsed bounds

    transform.destroy();

    transform = new OpenLayers.Control.TransformFeature(vectors, {
        rotate: false,
        irregular: true
    });

    transform.events.register("transformcomplete", transform, boxResize);

    map.addControl(transform);

    //transform.deactivate(); //The remove the box with handles
    vectors.destroyFeatures();

    //document.getElementById("bbox_drag_instruction").style.display = 'block';
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

function setBounds(bounds) {
    if (bounds == null) {
        var scope = angular.element('#bbox_result').scope();

        scope.$apply(function(){
            scope.topLeftLat = '';
            scope.topLeftLon = '';
            scope.bottomRightLat = '';
            scope.bottomRightLon = '';
        });


    } else {
        b = bounds.clone().transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));

        //Sending values to right scope in angular
        var scope = angular.element('#bbox_result').scope();
        scope.$apply(function(){
            scope.topLeftLat = b.top.toFixed(3);
            scope.bottomRightLat = b.bottom.toFixed(3);

            //wrap the world for lons
            if(b.left>180 || b.left<-180 ) scope.topLeftLon = (((b.left-(101*180))%360)+180).toFixed(3);
            else scope.topLeftLon = b.left.toFixed(3);

            if(b.right>180 || b.right<-180 ) scope.bottomRightLon = (((b.right-(101*180))%360)+180).toFixed(3);
            else scope.bottomRightLon = b.right.toFixed(3);
        });
    }
}

function handleDateLine(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon) {

    //if(topLeftLon>-180)

    var returnArray = new Array();
    if(Number(topLeftLon)>Number(bottomRightLon)) {
        //console.log('crossing date line');
        var bottomRightLonNumber = Number(bottomRightLon);
        bottomRightLonNumber+=360;
        bottomRightLon=bottomRightLonNumber.toString();

    }
    else {
        //console.log('not crossing date line');

    }
    returnArray=[topLeftLat, topLeftLon, bottomRightLat, bottomRightLon];
    return returnArray;
}

//Checks

function allValuesInputCheck(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon) {
    if(!isNaN(topLeftLat)&&!isNaN(topLeftLon)&&!isNaN(bottomRightLat)&&!isNaN(bottomRightLon)) return true;
    else return false;
}

