function timeSelection($scope,UrlService) {

    $scope.timeZones = timeZones;

    //Datepicker directive
    $scope.startDatepicker = startDatepicker;
    $scope.endDatepicker = endDatepicker;

    $scope.startTimepicker = startTimepicker;
    $scope.endTimepicker = endTimepicker;

    $scope.timeZone = timeZone;

    //$scope.datepicker = {date: new Date("2012-09-01T00:00:00.000Z"), setStartDate: new Date('2012-08-01T00:00:00.000Z')};

    $scope.invalidDateObject = false;

    //Watch for updates
    $scope.$watch('startDatepicker', updateStartTime, true);
    $scope.$watch('startTimepicker', updateStartTime, true);
    $scope.$watch('endDatepicker', updateEndTime, true);
    $scope.$watch('endTimepicker', updateEndTime, true);
    $scope.$watch('timeZone', updateTimeZone, true);

    //Build startTime string and send to service
    function updateStartTime(){
        var fromDate = '';

        //making from= string
        if($scope.startDatepicker.date != '' &&
            $scope.startTimepicker.time != ''){

            var tempDate = $scope.startDatepicker.date;
            var tempTime = $scope.startTimepicker.time;
            var tempTimeZone = $scope.timeZone.zone;

            try{
                var monthOfYear = tempDate.getMonth()+1;

                fromDate =  'startTime=' +
                    tempDate.getDate() + '.' +
                    monthOfYear + '.' +
                    tempDate.getFullYear() +'&';
            }
            catch(err){
                //do something maybe
            }
        }
        //Send to service
        UrlService.setFromDate(fromDate);
    }

    //Build endTime string and send to service
    function updateEndTime(){
        var toDate = '';
        //making to= string
        if($scope.endDatepicker.date != '' &&
                $scope.endTimepicker.time != ''){

                var tempDate = $scope.endDatepicker.date;
                var tempTime = $scope.endTimepicker.time;
                var tempTimeZone = $scope.timeZone.zone;

            try{
                var monthOfYear = tempDate.getMonth()+1;

                toDate =  'endTime=' +
                    tempDate.getDate() + '.' +
                    monthOfYear + '.' +
                    tempDate.getFullYear() +'&';
            }
            catch(err){
                //do something maybe
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
        console.log("Time is Money");
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

    //sending back the date of today
    $scope.returnNowDate = function() {
        //angular strap will not accept dynamic dates for start/end dates
        //nowdate will be today no matter what it is assigned to
        datedate = new Date();
        return datedate;
    }
}
