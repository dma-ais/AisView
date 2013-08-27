function dndCtrl($scope) {

    $scope.tableSeparators = [
        {ID: 'colon', Title: 'Colon', Value:':'},
        {ID: 'semi-colon', Title: 'Semicolon', Value:';'},
        {ID: 'comma', Title: 'Comma', Value:','},
        {ID: 'tab', Title: 'Tab', Value:'   '}
    ];

    $scope.tableSeparator = {sep: 'colon'};

    //TODO: change to service instead of pushing to root scope
    $scope.$root.tableSeparatorInRoot = $scope.tableSeparator;

    $scope.filterCategory = 'all';
    
    //Array of objects not included in query
    $scope.notIncluded = [
        { 'id': 0,	value: "Longitude",     queryName: "longitude",     ex: "12.369",       category: "all,xyz"},
        { 'id': 1, 	value: "Latitude",		queryName: "latitude",      ex: "55.634",       category: "all,xyz"},
        { 'id': 2, 	value: "Height",		queryName: "height",        ex: "22.12",        category: "all,xyz"},
        { 'id': 3, 	value: "Timeformat1",	queryName: "timeformat1",   ex: "YYYY-MM-DD",   category: "all,time"},
        { 'id': 4, 	value: "Timeformat2",	queryName: "timeformat2",   ex: "YYYYMMDD",     category: "all,time"},
        { 'id': 5, 	value: "Timeformat3",	queryName: "timeformat3",   ex: "DD-MM-YYYY",   category: "all,time"},
        { 'id': 6, 	value: "Signal1",		queryName: "signal1",       ex: "alpha",        category: "all,signal"},
        { 'id': 7, 	value: "Signal2",		queryName: "signal2",       ex: "beta",         category: "all,signal"},
        { 'id': 8, 	value: "Signal3",		queryName: "signal3",       ex: "charlie",      category: "all,signal"},
        { 'id': 9, 	value: "Signal4",		queryName: "signal4",       ex: "delta",        category: "all,signal"}];
 		
 	//Array of objects included in query
    $scope.included = [];

    //TODO: change to service instead of pushing to root scope
    $scope.$root.includedInRoot = $scope.included;

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

    $scope.headerPreview = function() {
        //filter tableSeparators array to find separator character
        var filteredArray = $scope.tableSeparators.filter(function (element) {
            return element.ID == $scope.tableSeparator.sep;
        });

        var breadcrumb = '';
        angular.forEach($scope.included, function(item) {
            breadcrumb+=item.queryName+filteredArray[0].Value;
        });
        //always remove last separator from string
        breadcrumb=breadcrumb.slice(0, -1);
        return breadcrumb;
    };
    $scope.exPreview = function() {
        //filter tableSeparators array to find separator character
        var filteredArray = $scope.tableSeparators.filter(function (element) {
            return element.ID == $scope.tableSeparator.sep;
        });

        var breadcrumb = '';
        angular.forEach($scope.included, function(item) {
            breadcrumb+=item.ex+filteredArray[0].Value;
        });
        //always remove last separator from string
        breadcrumb=breadcrumb.slice(0, -1);
        return breadcrumb;

    };
}