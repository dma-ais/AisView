function dndCtrl($scope) {
 
    $scope.notIncluded = [
        {	"id": 1, "value": "Longitude" },
        { "id": 2, "value": "Latitude"  },
        { "id": 3, "value": "Height"    },
        { "id": 4,  "value": "Time"     },
        { "id": 5, "value": "Signal" },
        { "id": 6, "value": "Timestamp"},
        { "id": 7, "value": "Altitude"},
        { "id": 8, "value": "Noname"}];
 
    $scope.included = [];
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("notIncluded", function(value) {
        console.log("NotIncluded: " + value.map(function(e){return e.id}).join(','));
    },true);
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("included", function(value) {
        console.log("Included: " + value.map(function(e){return e.id}).join(','));
    },true);
    
    $scope.includedEmpty = function() {
        return $scope.included.length == 0;
    }
 
    $scope.notIncludedEmpty = function() {
        return $scope.notIncluded.length == 0;
    }
}