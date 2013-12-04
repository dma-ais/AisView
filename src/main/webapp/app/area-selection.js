function areaSelection($scope,UrlService) {

    //Area variables
    $scope.topLeftLat;
    $scope.topLeftLon;
    $scope.bottomRightLat;
    $scope.bottomRightLon;

    //String holding warnings for odd input in map input fields
    $scope.mapInputWarning = '';

    //Sting to send to request URL
    var area = '';

    //validation if latitude is the same
    $scope.latitudeCheck = function(input){

        validAreaInput();

        if((parseFloat(input) > 90) || (parseFloat(input) < -90)) return true;
        else return false;
    }

    //validation if longitude is the same
    $scope.longitudeCheck = function(input){

        validAreaInput();

        if((parseFloat(input) > 180) || (parseFloat(input) < -180)) return true;
        else return false;
    }

    $scope.sameLatCheck = function(){
        validAreaInput();
        if(Number($scope.topLeftLat).toString() == Number($scope.bottomRightLat).toString() &&
            !isNaN($scope.topLeftLat) && !isNaN($scope.bottomRightLat) &&
            $scope.topLeftLat!='' && $scope.bottomRightLat!='') {
                return true;
        }
        else {
            return false;
        }
    }

    $scope.sameLonCheck = function(){
        validAreaInput();
        if(Number($scope.topLeftLon).toString() == Number($scope.bottomRightLon).toString() &&
            !isNaN($scope.topLeftLon) && !isNaN($scope.bottomRightLon) &&
            $scope.topLeftLon!='' && $scope.bottomRightLon!='') return true;
        else return false;
    }

    //validate if all inputted values in area-selection is ok (used for setting UrlService.allInputValidService)
    function validAreaInput(){
        //wrong lat input?
        var latVal, lonVal, latSameVal, lonSameVal;

        if( (parseFloat($scope.topLeftLat) > 90) || (parseFloat($scope.topLeftLat) < -90) ||
            (parseFloat($scope.bottomRightLat) > 90) || (parseFloat($scope.bottomRightLat) < -90)) {
            latVal=false;
        }
        else latVal=true;

        //wrong lon input?
        if((parseFloat($scope.topLeftLon) > 180) || (parseFloat($scope.topLeftLon) < -180) ||
            (parseFloat($scope.bottomRightLon) > 180) || (parseFloat($scope.bottomRightLon) < -180)) {
            console.log("lat ikke ok");
            lonVal=false;
        }
        else lonVal=true;

        //both lat the same
        if(Number($scope.topLeftLat).toString() == Number($scope.bottomRightLat).toString() &&
            !isNaN($scope.topLeftLat) && !isNaN($scope.bottomRightLat) &&
            $scope.topLeftLat!='' && $scope.bottomRightLat!='') latSameVal=false;
        else latSameVal=true;

        //both lon the same
        if(Number($scope.topLeftLon).toString() == Number($scope.bottomRightLon).toString() &&
            !isNaN($scope.topLeftLon) && !isNaN($scope.bottomRightLon) &&
            $scope.topLeftLon!='' && $scope.bottomRightLon!='') lonSameVal=false;
        else lonSameVal=true;

        //final test
        if  (latVal === true && lonVal === true && latSameVal === true && lonSameVal === true  ) UrlService.setAllInputValidService(true);
        else  UrlService.setAllInputValidService(false);
    }

    $scope.$watch('topLeftLat', updateURL);
    $scope.$watch('topLeftLon', updateURL);
    $scope.$watch('bottomRightLat', updateURL);
    $scope.$watch('bottomRightLon', updateURL);

    //
    function updateURL() {

        if ($scope.topLeftLat && $scope.topLeftLon && $scope.bottomRightLat && $scope.bottomRightLon) {
            area=area_restService+$scope.topLeftLat+','+$scope.topLeftLon+','+$scope.bottomRightLat+','+$scope.bottomRightLon+'&';
        }else area='';

        //Send to service
        UrlService.setArea(area);
    }

    $scope.changeMapInputField = function() {

        //remove insignificant trailing zeros from lat/lon input
        var topLeftLat,topLeftLon, bottomRightLat, bottomRightLon;
        if($scope.topLeftLat == '');
        else if ($scope.topLeftLat !== undefined) topLeftLat = Number($scope.topLeftLat).toString();

        if($scope.topLeftLon == '');
        else if($scope.topLeftLon !== undefined) topLeftLon = Number($scope.topLeftLon).toString();

        if($scope.bottomRightLat == '');
        else if ($scope.bottomRightLat !== undefined) bottomRightLat = Number($scope.bottomRightLat).toString();

        if($scope.bottomRightLon == '');
        else if ($scope.bottomRightLon !== undefined) bottomRightLon = Number($scope.bottomRightLon).toString();

        //send to mapFunctions.newInput()
        newInput(topLeftLat,topLeftLon,bottomRightLat,bottomRightLon);
    }
}

      