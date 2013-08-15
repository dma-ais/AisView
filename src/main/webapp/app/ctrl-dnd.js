function dndCtrl($scope) {
 
    $scope.filterCategory = 'all';
    
    //Array of objects not included in query
    $scope.notIncluded = [
        { 'id': 0,	value: "Longitude",		category: "all,xyz"},
        { 'id': 1, 	value: "Latitude",		category: "all,xyz"},
        { 'id': 2, 	value: "Height",			category: "all,xyz"},
        { 'id': 3, 	value: "Timeformat1",	category: "all,time"},
        { 'id': 4, 	value: "Timeformat2",	category: "all,time"},
        { 'id': 5, 	value: "Timeformat3",	category: "all,time"},
        { 'id': 6, 	value: "Signal1",			category: "all,signal"},
        { 'id': 7, 	value: "Signal2",			category: "all,signal"},
        { 'id': 8, 	value: "Signal3",			category: "all,signal"},
        { 'id': 9, 	value: "Signal4",			category: "all,signal"}];
 		
 		//Array of objects included in query
    $scope.included = [];
    
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("notIncluded", function(value) {
        //console.log("NotIncluded: " + value.map(function(e){return e.id}).join(','));
    },true);
 
    // watch, use 'true' to also receive updates when values
    // change, instead of just the reference
    $scope.$watch("included", function(value) {
        //console.log("Included: " + value.map(function(e){return e.id}).join(','));
    },true);
    
    $scope.includedEmpty = function() {
        return $scope.included.length == 0;
    }
 
    $scope.notIncludedEmpty = function() {
        return $scope.notIncluded.length == 0;
    }
}