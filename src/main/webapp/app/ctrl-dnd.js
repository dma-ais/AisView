function dndCtrl($scope) {
 
    $scope.filterCategory = 'xyz';
    
    function check(categoryString) {
  		$scope.filterCategory=categoryString;
  		console.log('catString: '+categoryString);
  }
    
    check
    
    $scope.notIncluded = [
        {	'id': 1,	value: "Longitude",		category: "xyz"},
        { 'id': 2, 	value: "Latitude",		category: "xyz"},
        { 'id': 3, 	value: "Height",			category: "xyz"},
        { 'id': 4, 	value: "Timeformat1",	category: "time"},
        { 'id': 5, 	value: "Timeformat2",	category: "time"},
        { 'id': 6, 	value: "Timeformat3",	category: "time"},
        { 'id': 7, 	value: "Signal1",			category: "signal"},
        { 'id': 8, 	value: "Signal2",			category: "signal"},
        { 'id': 9, 	value: "Signal3",			category: "signal"},
        { 'id': 10, 	value: "Signal4",		category: "signal"}];
 
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