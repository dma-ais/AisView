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

    $scope.latitudeCheck = function(input){
        console.log("Wrong lat");
        if((parseFloat(input) > 90) || (parseFloat(input) < -90)) return true;
        else return false;
    }

    $scope.sameLatCheck = function(){
        console.log("Same lat");
        if($scope.topLeftLat == $scope.bottomRightLat) return true;
        else return false;
    }

    $scope.sameLonCheck = function(){
        console.log("Same lon");
        if($scope.topLeftLon == $scope.bottomRightLon) return true;
        else return false;
    }




    //$scope.$watch('topLeftLat'+'topLeftLon'+'bottomRightLat'+'bottomRightLon', updateURL)

    $scope.$watch('topLeftLat', updateURL);
    $scope.$watch('topLeftLon', updateURL);
    $scope.$watch('bottomRightLat', updateURL);
    $scope.$watch('bottomRightLon', updateURL);

    //
    function updateURL() {

        if ($scope.topLeftLat && $scope.topLeftLon && $scope.bottomRightLat && $scope.bottomRightLon) {
            area='area='+$scope.topLeftLat+','+$scope.topLeftLon+','+$scope.bottomRightLat+','+$scope.bottomRightLon+'&';
        }else area='';

        //Send to service
        UrlService.setArea(area);
    }

    $scope.changeMapInputField = function() {

        //testMapBBox($scope.topLeftLat,$scope.topLeftLon,$scope.bottomRightLat,$scope.bottomRightLon);
        newInput($scope.topLeftLat,$scope.topLeftLon,$scope.bottomRightLat,$scope.bottomRightLon);

    }
}

      