function sourceFilters($scope,UrlService,$http) {

    //Controllers of number of new Base and Country input field
    var countBase = 2;
    var countRegion = 2;
    var countCountry = 2;
    var maxBaseChar = 9; //maximum number of characters when user input base stations

    //autocomplete for sourceCountries
    $scope.countryDatabase = [];
    $scope.countryCode = [];

    for (var i=0; i<countryCodes.length; i++) {
        $scope.countryDatabase[i]=countryCodes[i].name;
        $scope.countryCode[i]=countryCodes[i].code;

   }

    //Source Tab Headings: source data.js
    $scope.sourceIdTabHeader = tabHeadings[0];
    $scope.sourceBaseTabHeader = tabHeadings[1];
    $scope.sourceCountryTabHeader = tabHeadings[2];
    $scope.sourceTypeTabHeader = tabHeadings[3];
    $scope.sourceRegionTabHeader = tabHeadings[4];

    //Source ID variables fetched from: source /store/sourceIDs
    $scope.tempIds = [];
    var sourceIds = [];
    sourceIds[0]= {text:'All', value: 'all&', include:true};


    //building sourceID request
    var sourceIdsRequest = requestDomain + sourcecIdEndpoint;
    $http({method: 'GET', url: sourceIdsRequest}).success(function(data){
        $scope.tempIds = data.sourceIDs; // response data

        //building the sourceIDs array
        for (var i = 0; i < $scope.tempIds.length; i++) {
            sourceIds[i+1]= {text:$scope.tempIds[i], value: $scope.tempIds[i], include:false}
        }
    });

    //Source ID variables: source data.js
    $scope.sourceIds = sourceIds;


    //Source Base variables
    $scope.sourceBases = [{text:1+'.', input:'', counter: 1}];
    //Source Country variables
    $scope.sourceCountries = [{text:1+'.', input:'', counter: 1}];
    //Source Type variables
    $scope.sourceTypes = 'any';
    //Source Region variables
    $scope.sourceRegions = [{text:1+'.', input:'', counter: 1}];

    //Control of Source IDs
    $scope.sourceIdsSelect = function(sourceId) {

        //if all is selected, deselect all other
        if(sourceId.value==$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {

            angular.forEach($scope.sourceIds, function(sourceId) {
                sourceId.include=false;
            });

            $scope.sourceIds[0].include=true;
            $scope.sourceIdTabHeader = tabHeadings[0];

        }

        //deselect all if any other is selected
        if(sourceId.value!=$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {
            $scope.sourceIds[0].include=false;
            $scope.sourceIdTabHeader = tabHeadings[0]+'(*)';
        }
    }

    //Adding new text field for countries
    $scope.newTextFieldCountry = function(sourceCountry) {

        //Adding new text field
        var index = sourceCountry.counter;
        var testString = $scope.sourceCountries[index-1].input.toUpperCase();
        var indexOfCountry = $scope.countryDatabase.indexOf(testString);

        if(sourceCountry.counter === $scope.sourceCountries.length && indexOfCountry!=-1) {
            $scope.sourceCountries.push({text:countCountry+'.', input:'', counter: countCountry});
            countCountry++;
        }

        //Show that Source Country are edited if they are
        if(sourceCountry.input.length!=0) $scope.sourceCountryTabHeader = tabHeadings[2]+'(*)';

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
            $scope.sourceCountryTabHeader = tabHeadings[2];
        }
    };

    //Function to validate user country input
    $scope.checkCountryDB = function(sourceCountry) {
        //user input
        var testString = $scope.sourceCountries[sourceCountry.counter-1].input.toUpperCase();

        //test to see if user input is in country db
        var indexOfControlCountry = $scope.countryDatabase.indexOf(testString);

        //console.log('blur on text field '+index +' with input: '+testString+' with index ' +indexOfControlCountry +' in db');

        //Making boolean to control css-class
        if (testString.length != 0 && indexOfControlCountry===-1) return true;
        else return false;
    }

    //Adding new text field for bases
    $scope.newTextFieldBase = function(sourceBase) {

        //check max char
        if(sourceBase.input.length===maxBaseChar+1) {
            temp = sourceBase.input;
            temp = temp.slice(0,temp.length - 1);
            sourceBase.input=temp;
        }
        //console.log('length of ?: '+sourceBase.input.length);

        if(sourceBase.counter === $scope.sourceBases.length) {
            $scope.sourceBases.push({text:countRegion+'.', input:'', counter: countRegion});
            countRegion++;
        }

        //Show that Source Base are edited
        if(sourceBase.input.length!=0) $scope.sourceBaseTabHeader = tabHeadings[1]+'(*)';

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
            $scope.sourceBaseTabHeader = tabHeadings[1];
        }
    };

    //Adding new text field for regions
    $scope.newTextFieldRegion = function(sourceRegion) {

        if(sourceRegion.counter === $scope.sourceRegions.length) {
            $scope.sourceRegions.push({text:countBase+'.', input:'', counter: countBase});
            countBase++;
        }

        //Show that Source Base are edited
        if(sourceRegion.input.length!=0) $scope.sourceRegionTabHeader = tabHeadings[4]+'(*)';

        //Control to remove (*) if all textfields are empty
        var allEmpty = true;
        var keepGoing = true;
        angular.forEach($scope.sourceRegions, function(item) {
            if (keepGoing && (item.input.length!==0)) {
                allEmpty = false;
                keepGoing = false;
            }
        });

        if(allEmpty) {
            $scope.sourceRegionTabHeader = tabHeadings[4];
        }
    };

    //Control of Source Types Tab header
    $scope.sourceTypeHeadingControl = function() {

        if($scope.sourceTypes!='any'){
            $scope.sourceTypeTabHeader = tabHeadings[3]+'(*)';
        }
        else $scope.sourceTypeTabHeader = tabHeadings[3];
    }

    //Clear all Source filtering entries
    $scope.clearSources = function() {

        //restore all TabHeaders
        $scope.sourceIdTabHeader = tabHeadings[0];
        $scope.sourceBaseTabHeader = tabHeadings[1];
        $scope.sourceCountryTabHeader = tabHeadings[2];
        $scope.sourceTypeTabHeader = tabHeadings[3];
        $scope.sourceRegionTabHeader = tabHeadings[4];

        //set all source IDs to false
        angular.forEach($scope.sourceIds, function(item) {
            item.include=false;
        });
        //and 'all' to true
        _.first($scope.sourceIds).include=true;
        //clear source bases
        $scope.sourceBases = [
            {text:1+'.', input:'', counter: 1}];
        countBase = 2;
        //clear source countries
        $scope.sourceCountries = [
            {text:1+'.', input:'', counter: 1}];
        countCountry = 2;
        //clear source regions
        $scope.sourceRegions = [
            {text:1+'.', input:'', counter: 1}];
        countRegion = 2;

        $scope.sourceTypes='any';
    }

    //Include all text from custom number of dynamic text fields
    function includeFromTextField(array,baseString) {
        var returnString;
        var someInput = false;
        //append all source bases
        angular.forEach(array, function(item) {
            if(item.input.length>0) someInput=true;
        });
        if(someInput) {
            returnString=baseString;
            angular.forEach(array, function(item) {
                 if(item.input.length>0) returnString+=item.input+',';
            });
        }else returnString='';

        //replace , with & at end of string
        returnString=returnString.replace(/^,|,$/g,'&');

        return returnString;
    }

    //Include all text from custom number of dynamic text fields if input is in database
    function includeFromTextFieldWithDB(array,dbArray,baseString) {
        var returnString;

        //any output and any true country input?
        var someInput = false;
        var trueInput = false;
        var indexOfCountry;

        angular.forEach(array, function(item) {
            indexOfCountry = dbArray.indexOf(item.input.toUpperCase());
            //check input length
            if(item.input.length>0) someInput=true;
            //check input quality
            if(indexOfCountry!=-1) trueInput = true;

        });
        //append all input which are in database
        if(someInput && trueInput) {
            returnString=baseString;

            var indexOfCountry;
            angular.forEach(array, function(item) {
                indexOfCountry = dbArray.indexOf(item.input.toUpperCase());
                if(item.input.length>0 && indexOfCountry!=-1) returnString+=$scope.countryCode[indexOfCountry]+',';
            });
        }else returnString='';

        //replace , with & at end of string
        returnString=returnString.replace(/^,|,$/g,'&');

        return returnString;
    }

    //
    //
    //
    var ids ='';
    var bases ='';
    var countries='';
    var types='';
    var regions='';


    //If sourceIds array are changed push to service
    $scope.$watch('sourceIds', function() {
        //var ids = '';
        //append all source IDs if All is not selected else no ids in query
        if(_.first($scope.sourceIds).include==true) ids = '';
        else {
            ids='id=';
            angular.forEach($scope.sourceIds, function(sourceId) {
                if (sourceId.include) ids+=sourceId.value+',';
            });
        }
        //delete , with & at end of string
        ids=ids.replace(/^,|,$/g,'&');

        //Send to service
        pushToService();

    }, true); // <-- objectEquality

    //If sourceBases array are changed push to service
    $scope.$watch('sourceBases', function() {
        bases = includeFromTextField($scope.sourceBases,'bs=');
        //Send to service
        pushToService();
    }, true); // <-- objectEquality

    //If sourceCountries array are changed push to service
    $scope.$watch('sourceCountries', function() {
        countries=includeFromTextFieldWithDB($scope.sourceCountries,$scope.countryDatabase,'country=');
        //Send to service
        pushToService();
    }, true); // <-- objectEquality

    //If sourceTypes array are changed push to service
    $scope.$watch('sourceTypes', function() {

        //var types = '';
        if($scope.sourceTypes=='any') types='';
        else types='type='+$scope.sourceTypes+'&'
        //Send to service
        pushToService();
    });

    //If sourceRegions array are changed push to service
    $scope.$watch('sourceRegions', function() {
        regions=includeFromTextField($scope.sourceRegions,'region=');
        //Send to service
        pushToService();
    }, true); // <-- objectEquality


    function pushToService(){

        var message = source_restService + ids + bases + countries + types + regions;
        if(message === source_restService) message='';

        //Send to service
        UrlService.setSourceFiltering(message);

    }
}
