function SourceIds($scope) {

    $scope.mapInputWarning = '';

    $scope.headerChecked = '';

    $scope.sourceIdTabHeader = 'Source ID';
    $scope.sourceBaseTabHeader = 'Source Base Station';
    $scope.sourceCountryTabHeader = 'Source Country';
    $scope.sourceTypeTabHeader = 'Source Type';
    $scope.sourceRegionTabHeader = 'Source Region';



    //Map variables
	$scope.topLeftLat;
	$scope.topLeftLon;
	$scope.bottomRightLat;
	$scope.bottomRightLon;



    //Source variables
    $scope.sourceIds = [
  	    {text:'All', value: 'all&', include:true},
        {text:'Source1', value: 'src1', include:false},
        {text:'Source2', value: 'src2', include:false},
        {text:'Source3', value: 'src3', include:false},
        {text:'Source4', value: 'src4', include:false},
        {text:'Source5', value: 'src5', include:false},
        {text:'Source6', value: 'src6', include:false}];


    $scope.sourceIdsSelect = function(sourceId) {
		
	    //if all is selected, deselect all other
		if(sourceId.value==$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {

            angular.forEach($scope.sourceIds, function(sourceId) {
      	        sourceId.include=false;
    	    });

            $scope.sourceIds[0].include=true;
            $scope.sourceIdTabHeader = 'Source ID';

        }
		
		//deselect all if any other is selected	
		if(sourceId.value!=$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {
			$scope.sourceIds[0].include=false;
            $scope.sourceIdTabHeader = 'Source ID (*)';
		}
    }
    
    $scope.sourceBases = [
        {text:1+'.', input:'', counter: 1}];
    
    $scope.sourceCountries = [
        {text:1+'.', input:'', counter: 1}];
    
    $scope.sourceTypes = 'any';
  
    //Area variables
    $scope.bboxResult = '';
  
    $scope.format = '';
  
    $scope.samples = '';
    
	//making final request url
    $scope.url = function() {
        var base = 'http://www.example.com/';
        var ids = '';
        var bases = '';
        var countries = '';
        var types = '';
        var samples = '';
        var area = '';
        var fromDate = '';
        var toDate = '';
        var tables = '';
        var separator = '';


        //append all source IDs if All is not selected else no ids in query
        if(_.first($scope.sourceIds).include==true) ids = '';
        else {
            ids='source=';
            angular.forEach($scope.sourceIds, function(sourceId) {
            if (sourceId.include) ids+=sourceId.value+',';
            });
        }
        //delete , with & at end of string
        ids=ids.replace(/^,|,$/g,'&');

        //build string from text fields (base and country)
        bases = includeFromTextField($scope.sourceBases,'bs=');
        countries = includeFromTextField($scope.sourceCountries,'ctry=');

        //build string of source types
        if($scope.sourceTypes=='any') types='';
        else types='type='+$scope.sourceTypes+'&'

        //build string of samples
        if($scope.samples=='') samples='';
        else samples='samples='+$scope.samples+'&';

        if ($scope.topLeftLat != null &&
                $scope.topLeftLon != null &&
                $scope.bottomRightLat != null &&
                $scope.bottomRightLon != null) {
            area='area='+$scope.topLeftLat+','+$scope.topLeftLon+','+$scope.bottomRightLat+','+$scope.bottomRightLon+'&';
        }else area='';

        //append all tables if format is 'table' else no tables
        if($scope.format=='') tables = '';
        else {
            //only append if included tables list is not empty
            if($scope.$root.includedInRoot.length!=0){
                tables='tables=';
                angular.forEach($scope.$root.includedInRoot, function(includedItem) {
                    tables+=includedItem.queryName+',';
                });

                //delete , with & at end of string
                tables=tables.replace(/^,|,$/g,'&');
            }
        }

        //appending the separator
        if($scope.format=='') separator = '';
        else {
            //only append if included tables list is not empty
            if($scope.$root.includedInRoot.length!=0) separator='separator='+$scope.$root.tableSeparatorInRoot+'&';
        }

        //making from= string
        if($scope.$root.startDatepickerInRoot.date != '' &&
           $scope.$root.startTimepickerInRoot.time != ''){

            var tempDate = $scope.$root.startDatepickerInRoot.date;
            var tempTime = $scope.$root.startTimepickerInRoot.time;
            var tempTimeZone = $scope.$root.startTimeZoneInRoot.zone;

            var monthOfYear = tempDate.getMonth()+1;

            fromDate =  'from=' +
                        tempDate.getDate() + '.' +
                        monthOfYear + '.' +
                        tempDate.getFullYear() + ' - ' +
                        tempTime+ '(' + tempTimeZone + ')&';
        }
        //console.log('$scope.startTimeZoneSelected'+$scope.$scope.startTimeZoneSelected);

        //making to= string
        if($scope.$root.endDatepickerInRoot.date != '' &&
            $scope.$root.endTimepickerInRoot.time != ''){

            var tempDate = $scope.$root.endDatepickerInRoot.date;
            var tempTime = $scope.$root.endTimepickerInRoot.time;
            var tempTimeZone = $scope.$root.endTimeZoneInRoot.zone;

            var monthOfYear = tempDate.getMonth()+1;

            toDate =  'to=' +
                tempDate.getDate() + '.' +
                monthOfYear + '.' +
                tempDate.getFullYear() + ' - ' +
                tempTime + '(' + tempTimeZone + ')&';;
        }

        var almostFinalString = base+
                                ids+
                                bases+
                                countries+
                                types+
                                area+
                                fromDate+
                                toDate+
                                tables+
                                separator+
                                $scope.headerChecked+
                                samples;

        var finalString = base;

        if (almostFinalString.slice(-1)=='&') {
            finalString=almostFinalString.slice(0,-1);
        }

        //finalString=finalString.replace(/^'&'|,$/g,'');

        return 	finalString;
    };
  
    function includeFromTextField(array,baseString) {
  	    var returnString;
  	    var someInput = false;
        //append all source bases
        angular.forEach(array, function(sourceBase) {
            if(sourceBase.input.length>0) someInput=true;
        });
        if(someInput) {
    	    returnString=baseString;
    	    angular.forEach(array, function(sourceBase) {
      	    if(sourceBase.input.length>0) returnString+=sourceBase.input+',';
            });
        }else returnString='';
    
        //replace , with & at end of string
        returnString=returnString.replace(/^,|,$/g,'&');

        return returnString;
    }

    var countBase = 2;
    var countCountry = 2;

    $scope.newTextFieldBase = function(sourceBase) {
  	
        if(sourceBase.counter === $scope.sourceBases.length) {
            $scope.sourceBases.push({text:countBase+'.', input:'', counter: countBase});
            countBase++;
        }

        //Show that Source Base are edited
        if(sourceBase.input.length!=0) $scope.sourceBaseTabHeader = 'Source Base Station(*)';

        //Control to remove (*) if all textfields are empty
        var allEmpty = true;
        var keepGoing = true;
        angular.forEach($scope.sourceBases, function(item) {
            if (keepGoing && (item.input.length!==0)) {
                allEmpty = false;
                keepGoing = false;
            }
        });

        if(allEmpty) {
            $scope.sourceBaseTabHeader = 'Source Base Station';
        }
    };

  
    $scope.newTextFieldCountry = function(sourceCountry) {
  	

        if(sourceCountry.counter === $scope.sourceCountries.length) {
            $scope.sourceCountries.push({text:countCountry+'.', input:'', counter: countCountry});
  	        countCountry++;
        }
        //Show that Source Country are edited if they are
        if(sourceCountry.input.length!=0) $scope.sourceCountryTabHeader = 'Source Country(*)';

        //Control to remove (*) if all textfields are empty
        var allEmpty = true;
        var keepGoing = true;
        angular.forEach($scope.sourceCountries, function(item) {
            if (keepGoing && (item.input.length!==0)) {
                allEmpty = false;
                keepGoing = false;
            }
        });

        if(allEmpty) {
            $scope.sourceCountryTabHeader = 'Source Country';
        }

    };
  
    $scope.clearSources = function() {

        //restore all TabHeaders
        $scope.sourceIdTabHeader = 'Source ID';
        $scope.sourceBaseTabHeader = 'Source Base Station';
        $scope.sourceCountryTabHeader = 'Source Country';
        $scope.sourceTypeTabHeader = 'Source Type';
        $scope.sourceRegionTabHeader = 'Source Region';

        //set all source IDs to false
        angular.forEach($scope.sourceIds, function(item) {
            item.include=false;
        });
        //and 'all' to true
        _.first($scope.sourceIds).include=true;



        $scope.sourceBases = [
            {text:1+'.', input:'', counter: 1}];
        countBase = 2;

        $scope.sourceCountries = [
            {text:1+'.', input:'', counter: 1}];
        countCountry = 2;

        $scope.sourceTypes='any';
    }
  
    $scope.sourceTypeHeadingControl = function() {

        if($scope.sourceTypes!='any'){
            $scope.sourceTypeTabHeader = 'Source Type(*)';
        }
        else $scope.sourceTypeTabHeader = 'Source Type';
    }

    $scope.changeMapInputField = function() {

        //testMapBBox($scope.topLeftLat,$scope.topLeftLon,$scope.bottomRightLat,$scope.bottomRightLon);
        newInput($scope.topLeftLat,$scope.topLeftLon,$scope.bottomRightLat,$scope.bottomRightLon);

    }
};

function SourceTypes($scope) {
    
}