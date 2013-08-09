function dndCtrl($scope) {
 
    $scope.model = [
        {	"id": 1, "value": "Longitude" },
        { "id": 2, "value": "Latitude"  },
        { "id": 3, "value": "Height"    },
        { "id": 4,  "value": "Time"     }];
 
    $scope.source = [
        { "id": 5, "value": "Signal" },
        { "id": 6, "value": "Timestamp"},
        { "id": 7, "value": "Altitude"},
        { "id": 8, "value": "Noname"}];
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("model", function(value) {
        console.log("Model: " + value.map(function(e){return e.id}).join(','));
    },true);
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("source", function(value) {
        console.log("Source: " + value.map(function(e){return e.id}).join(','));
    },true);
    
    $scope.sourceEmpty = function() {
        return $scope.source.length == 0;
    }
 
    $scope.modelEmpty = function() {
        return $scope.model.length == 0;
    }
}