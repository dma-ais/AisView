function sourceFilters($scope,UrlService) {

    $scope.textValidator = 'false';

    //trying autocomplete for sourceCountries
    $scope.countryDatabase = [];
    $scope.countryCode = [];

    for (var i=0; i<countryCodes.length; i++) {
        $scope.countryDatabase[i]=countryCodes[i].name;
        $scope.countryCode[i]=countryCodes[i].code;

   }

    angular.forEach($scope.landcodes, function(item) {
        console.log(item.name);
        $scope.names[landCounter]=item.name;
    });

    //Source Tab Headings
    $scope.sourceIdTabHeader = 'Source ID';
    $scope.sourceBaseTabHeader = 'Source Base Station';
    $scope.sourceCountryTabHeader = 'Source Country';
    $scope.sourceTypeTabHeader = 'Source Type';
    $scope.sourceRegionTabHeader = 'Source Region';

    //Source ID variables
    $scope.sourceIds = [
        {text:'All', value: 'all&', include:true},
        {text:'Source1', value: 'src1', include:false},
        {text:'Source2', value: 'src2', include:false},
        {text:'Source3', value: 'src3', include:false},
        {text:'Source4', value: 'src4', include:false},
        {text:'Source5', value: 'src5', include:false},
        {text:'Source6', value: 'src6', include:false}];

    //Source Base variables
    $scope.sourceBases = [
        {text:1+'.', input:'', counter: 1}];

    //Source Country variables
    $scope.sourceCountries = [
        {text:1+'.', input:'', counter: 1}];

    //Source Type variables
    $scope.sourceTypes = 'any';

    //Controllers of number of new Base and Country input field
    var countBase = 2;
    var countCountry = 2;

    //Control of Source IDs
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
            $scope.sourceIdTabHeader = 'Source ID(*)';
        }
    }

    //Adding new text field for countries
    $scope.newTextFieldCountry = function(sourceCountry) {

        //Adding new text field
        var index = sourceCountry.counter;
        var testString = $scope.sourceCountries[index-1].input.toUpperCase();
        var indexOfCountry = $scope.countryDatabase.indexOf(testString);

        if(indexOfCountry!=-1) $scope.textValidator = false;

        if(sourceCountry.counter === $scope.sourceCountries.length && indexOfCountry!=-1) {
            $scope.sourceCountries.push({text:countCountry+'.', input:'', counter: countCountry});
            countCountry++;
        }

        //if(sourceCountry.counter === $scope.sourceCountries.length) {
        //    $scope.sourceCountries.push({text:countCountry+'.', input:'', counter: countCountry});
        //    countCountry++;
        //}

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
    $scope.controlTextFieldCountry = function(sourceCountry) {
        var index = sourceCountry.counter;
        var testString = $scope.sourceCountries[index-1].input.toUpperCase();

        console.log('blur on '+index +' '+testString+'!');

        var indexOfControlCountry = $scope.countryDatabase.indexOf(testString);
        console.log('blur on '+indexOfControlCountry);
        if (indexOfControlCountry===-1) {
            console.log('not at valid country');
            $scope.textValidator = true;
            //$scope.sourceCountries[index-1].input='';
        }else $scope.textValidator = false;
    }

    //Adding new text field for bases
    $scope.newTextFieldBase = function(sourceBase) {

        //check max char
        maxChar = 9;
        if(sourceBase.input.length===maxChar+1) {
            temp = sourceBase.input;
            temp = temp.slice(0,temp.length - 1);
            sourceBase.input=temp;
        }
        console.log('length of ?: '+sourceBase.input.length);

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

    //Control of Source Types Tab header
    $scope.sourceTypeHeadingControl = function() {

        if($scope.sourceTypes!='any'){
            $scope.sourceTypeTabHeader = 'Source Type(*)';
        }
        else $scope.sourceTypeTabHeader = 'Source Type';
    }

    //Clear all Source filtering entries
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

    //If sourceIds array are changed push to service
    $scope.$watch('sourceIds', function() {
        var ids = '';
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

        //Send to service
        UrlService.setIds(ids);
    }, true); // <-- objectEquality

    //If sourceBases array are changed push to service
    $scope.$watch('sourceBases', function() {
        //Send to service
        UrlService.setBases(includeFromTextField($scope.sourceBases,'bs='));
    }, true); // <-- objectEquality

    //If sourceCountries array are changed push to service
    $scope.$watch('sourceCountries', function() {

        //Send to service
        UrlService.setCountries(includeFromTextFieldWithDB($scope.sourceCountries,$scope.countryDatabase,'ctry='));
    }, true); // <-- objectEquality

    //If sourceTypes array are changed push to service
    $scope.$watch('sourceTypes', function() {

        var types = '';
        if($scope.sourceTypes=='any') types='';
        else types='type='+$scope.sourceTypes+'&'

        //Send to service
        UrlService.setTypes(types);
    });

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
                console.log('led efter2: '+item.input);
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
        var someInput = false;
        //append all source bases
        angular.forEach(array, function(item) {
            if(item.input.length>0) someInput=true;
        });
        if(someInput) {
            returnString=baseString;

            var indexOfCountry;
            angular.forEach(array, function(item) {
                console.log('led efter: '+item.input);
                indexOfCountry = dbArray.indexOf(item.input.toUpperCase());
                console.log('indexOfCountry: '+indexOfCountry);
                if(item.input.length>0 && indexOfCountry!=-1) returnString+=$scope.countryCode[indexOfCountry]+',';
            });
        }else returnString='';

        //replace , with & at end of string
        returnString=returnString.replace(/^,|,$/g,'&');

        return returnString;
    }
}
