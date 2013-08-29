function dndCtrl($scope,UrlService) {

    //Control of header included in query
    $scope.headerChecked = false;

    //Control of format
    $scope.format = '';

    $scope.tableSeparators = [
        {ID: 'colon', Title: 'Colon', Value:':'},
        {ID: 'semi-colon', Title: 'Semicolon', Value:';'},
        {ID: 'comma', Title: 'Comma', Value:','},
        {ID: 'tab', Title: 'Tab', Value:'   '}
    ];

    $scope.tableSeparator = {sep: 'colon'};

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

    //Make the example of header line
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

    //Make the example of query line
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
    //If included array are changed push to service
    $scope.$watch('included', function() {
        var tables = '';
        var separator = '';
        console.log('inside watch: '+$scope.included.length!==0);
        //append all tables if format is 'table' else no tables
        if($scope.format=='') {
            tables = '';
            separator = '';
        }
        else {
            //only append if included tables list is not empty
            if($scope.included.length!=0){
                tables='tables=';
                angular.forEach($scope.included, function(includedItem) {
                    tables+=includedItem.queryName+',';
                });

                //delete , with & at end of string
                tables=tables.replace(/^,|,$/g,'&');

                separator='separator='+$scope.tableSeparator.sep+'&';
            }
        }

        //Send to service
        UrlService.setTables(tables);
        UrlService.setSeparator(separator);
    }, true);

    //If tableSeparator are changed push to service
    $scope.$watch('tableSeparator', function() {
        var separator = '';

        if($scope.format=='') separator = '';
        else {
            //only append if included tables list is not empty
            if($scope.included.length!=0) separator='separator='+$scope.tableSeparator.sep+'&';
        }
        //Send to service
        UrlService.setSeparator(separator);
    }, true); // <-- objectEquality

    //If headerChecked are changed push to service
    $scope.$watch('headerChecked', function() {

        var header = '';
        if($scope.included.length!=0 && $scope.headerChecked) header = 'header=true&';
            else header = '';

        //Send to service
        UrlService.setHeader(header);
    });

}