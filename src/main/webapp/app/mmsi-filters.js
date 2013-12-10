function mmsiFilters($scope,UrlService,$http) {

    //Controllers of number of new mmsi input field
    var countMmsi = 2;

    //Source Region variables
    $scope.mmsis = [{text:1+'.', input:'', counter: 1}];

    //Adding new text field for regions
    $scope.newTextFieldMmsi = function(mmsi) {

        if(mmsi.counter === $scope.mmsis.length) {
            $scope.mmsis.push({text:countMmsi+'.', input:'', counter: countMmsi});
            countMmsi++;
        }

    };

    //Clear all Source filtering entries
    $scope.clearSources = function() {
        //clear source regions
        $scope.mmsis = [
            {text:1+'.', input:'', counter: 1}];
        countMmsi = 2;

    }

    //If mmsi array are changed push to service
    $scope.$watch('mmsis', function() {
        //Send to service
        UrlService.setMmsi(includeFromTextField($scope.mmsis,mmsi_restService));

    }, true); // <-- objectEquality

    //Include all text from custom number of dynamic text fields
    function includeFromTextField(array,baseString) {
        var returnString ='';
        var someInput = false;
        //append all source bases
        angular.forEach(array, function(item) {
            if(item.input.length>0) someInput=true;
        });
        if(someInput) {
            //returnString=baseString;

            angular.forEach(array, function(item) {
                if(item.input.length>0) returnString+=baseString+item.input+'&';
            });
        }else returnString='';

        //replace , with & at end of string
        returnString=returnString.replace(/^,|,$/g,'&');

        return returnString;
    }
}
