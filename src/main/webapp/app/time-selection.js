function timeSelection($scope,UrlService) {

    var testcounter=0;

    $scope.timeZones = timeZones;

    //Getting initial values from data.ja
    $scope.startDatepicker = startDatepicker;
    $scope.endDatepicker = endDatepicker;

    $scope.startTimepicker = startTimepicker;
    $scope.endTimepicker = endTimepicker;

    $scope.timeZone = timeZone;

    //Watch for updates
    $scope.$watch('startDatepicker', updateStartTime, true);
    $scope.$watch('startTimepicker', updateStartTime, true);
    $scope.$watch('endDatepicker', updateEndTime, true);
    $scope.$watch('endTimepicker', updateEndTime, true);
    $scope.$watch('timeZone', updateTimeZone, true);

    //Build startTime string and send to service
    function updateStartTime(){
        //called to set UrlService.areaValidService
        validTimeInput();

        var fromDate = '';

        //making from= string  http://en.wikipedia.org/wiki/ISO_8601
        if($scope.startDatepicker.date != '' &&
            $scope.startTimepicker.time != ''){

            var tempDate = $scope.startDatepicker.date;
            var tempTime = $scope.startTimepicker.time;
            var tempTimeZone = $scope.timeZone.zone;

            try{
                var monthOfYear = tempDate.getMonth()+1;

                fromDate =  time_restService +
                    tempDate.getFullYear() + '-' +
                    monthOfYear + '-' +
                    tempDate.getDate() +
                    'T' + tempTime + ':' +
                    '00Z';
            }
            catch(err){
                //do something maybe
                console.log("TimeError: " +err);
            }
        }
        //Send to service
        UrlService.setFromDate(fromDate);
    }

    //Build endTime string and send to service
    function updateEndTime(){
        //called to set UrlService.areaValidService
        validTimeInput();

        var toDate = '';
        //making to= string http://en.wikipedia.org/wiki/ISO_8601
        if($scope.endDatepicker.date != '' &&

            $scope.endTimepicker.time != ''){

            var tempDate = $scope.endDatepicker.date;
            var tempTime = $scope.endTimepicker.time;
            var tempTimeZone = $scope.timeZone.zone;

            try{
                var monthOfYear = tempDate.getMonth()+1;

                toDate =  '/' +
                    tempDate.getFullYear() + '-' +
                    monthOfYear + '-' +
                    tempDate.getDate() +
                    'T' + tempTime + ':' +
                '00Z'+
                '&';
            }
            catch(err){
                //do something maybe
                console.log("TimeError "+err);

            }
        }
        //Send to service
        UrlService.setToDate(toDate);
    }

    //Build timeZone string and send to service
    function updateTimeZone(){
        var timeZoneStr= 'timeZone=' + $scope.timeZone.ID + '&';

        //Send to service
        UrlService.setTimeZone(timeZoneStr);

    }

    //
    //Validation of dates
    //

    //Check if date is en future
    $scope.checkIfFutureDate = function(date) {
        var startDate = new Date(date);
        var currentDate = new Date();

        //Making boolean to control css-class
        //We want only past dates
        if (currentDate<startDate) {
            //This is a future date
            return true;
        }
        else {
            //This is a past date
            return false;
        }
    }
    //Check if end date is after start date
    $scope.afterStartDate = function() {
        var startDate = new Date($scope.startDatepicker.date)
        var endDate = new Date($scope.endDatepicker.date);

        //Making boolean to control css-class
        //We want only past dates
        if (startDate>endDate) {
            //Start date after end date->what we dont want
            return true;
        }
        else {
            //Start date before end date->what we want
            return false;
        }
    }
    //Check to see if user entered string is a valid date format
    $scope.validDate = function(date) {

        var timestamp=Date.parse(date)

        if (isNaN(timestamp)==false) return false;
        else return true;
    }

    //
    //Validation of times
    //
    //If start/end dates are equal check start/end times
    $scope.afterStartTime = function() {
        if($scope.startDatepicker.date.getDate() === $scope.endDatepicker.date.getDate()){
            //console.log("both dates are the same");
            if ($scope.startTimepicker.time>=$scope.endTimepicker.time) {
                //Start time after end time->what we dont want
                //console.log("start time after end time :(");
                return true;
            }
            else {
                //Start time before end time->what we want
                //console.log("start time before end time :)");
                return false;
            }
        }
        else {
            //console.log("both dates are NOT the same");
            return false;
        }

    }

    //validate if all inputted values in time-selection is ok
    //Used for setting UrlService.areaValidService
    function validTimeInput(){
        var futureVal, afterStartVal, afterStartTimeVal, formatVal;

        //checj if future dates
        if($scope.checkIfFutureDate($scope.startDatepicker.date) || $scope.checkIfFutureDate($scope.endDatepicker.date)) futureVal=false;
        else futureVal=true;

        //check if startDate is past endDate
        if($scope.afterStartDate()) afterStartVal=false;
        else afterStartVal=true;

        ////check if startTime is past endTime
        if($scope.afterStartTime()) afterStartTimeVal=false;
        else afterStartTimeVal=true;

        //Invalid input
        if($scope.validDate($scope.startDatepicker.date) || $scope.validDate($scope.endDatepicker.date)) formatVal=false;
        else formatVal=true;


        //final test
        if  (futureVal === true &&
            afterStartVal === true &&
            afterStartTimeVal === true &&
            formatVal === true ) UrlService.setTimeValidService(true);
        else{
            UrlService.setTimeValidService(false);
        }
    }

    //sending back the date of today
    $scope.returnNowDate = function() {
        //angular strap will not accept dynamic dates for start/end dates
        //nowdate will be today no matter what it is assigned to
        datedate = new Date();
        return datedate;
    }
}
