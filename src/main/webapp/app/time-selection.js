function timeSelection($scope,UrlService) {

    $scope.timeZones = [
        {ID: 'utc', Title: 'UTC'},
        {ID: 'utc/gmt+1', Title: 'UTC/GMT+1'},
        {ID: 'utc/gmt+2', Title: 'UTC/GMT+2'},
        {ID: 'utc/gmt+3', Title: 'UTC/GMT+3'},
        {ID: 'utc/gmt+4', Title: 'UTC/GMT+4'}
    ];

    // Datepicker directive
    $scope.startDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};
    $scope.endDatepicker = {date: new Date("2012-09-10T00:00:00.000Z")};

    $scope.startTimepicker = {time: "00:00 AM"};
    $scope.endTimepicker = {time: "00:00 AM"};

    $scope.startTimeZone = {zone: 'utc'};
    $scope.endTimeZone = {zone: 'utc'};

    $scope.datepicker = {date: new Date("2012-09-01T00:00:00.000Z"), setStartDate: new Date('2012-08-01T00:00:00.000Z')};


    //Watch for updates
    $scope.$watch('startDatepicker', updateStartTime, true);
    $scope.$watch('startTimepicker', updateStartTime, true);
    $scope.$watch('endDatepicker', updateEndTime, true);
    $scope.$watch('endTimepicker', updateEndTime, true);

    //Build string and end to service
    function updateStartTime(){
        var fromDate = '';
        console.log('11');
        //making from= string
        if($scope.startDatepicker.date != '' &&
            $scope.startTimepicker.time != ''){
            console.log('22');

            var tempDate = $scope.startDatepicker.date;
            var tempTime = $scope.startTimepicker.time;
            var tempTimeZone = $scope.startTimeZone.zone;

            var monthOfYear = tempDate.getMonth()+1;

            fromDate =  'from=' +
                tempDate.getDate() + '.' +
                monthOfYear + '.' +
                tempDate.getFullYear() + ' - ' +
                tempTime+ '(' + tempTimeZone + ')&';
        }
        //Send to service
        UrlService.setFromDate(fromDate);
    }

    //Build string and end to service
    function updateEndTime(){
        var toDate = '';
        //making to= string
        if($scope.endDatepicker.date != '' &&
                $scope.endTimepicker.time != ''){

                var tempDate = $scope.endDatepicker.date;
                var tempTime = $scope.endTimepicker.time;
                var tempTimeZone = $scope.endTimeZone.zone;

                var monthOfYear = tempDate.getMonth()+1;

                toDate =  'to=' +
                    tempDate.getDate() + '.' +
                    monthOfYear + '.' +
                    tempDate.getFullYear() + ' - ' +
                    tempTime + '(' + tempTimeZone + ')&';;
        }
        //Send to service
        UrlService.setToDate(toDate);
    }
    //
    //Validation of dates
    //
    //Check if date is en future
    $scope.checkIfFutureDate = function() {
        var startDate = new Date($scope.startDatepicker.date);
        var currentDate = new Date();

        console.log('check date: input: '+startDate+'now:'+currentDate);



        //Making boolean to control css-class
        //We want only past dates
        if (currentDate<startDate) {
            console.log('future');
            return true;
        }
        else {
            console.log('past');
            return false;
        }
    }
    //Check if end date is after start date
    $scope.afterStartDate = function() {
        var startDate = new Date($scope.startDatepicker.date)
        var endDate = new Date($scope.endDatepicker.date);
        //var currentDate = new Date();

        //console.log('check date: input: '+inputDate+'now:'+currentDate);



        //Making boolean to control css-class
        //We want only past dates
        if (startDate>endDate) {
            console.log('startDate after endDate :(');
            return true;
        }
        else {
            console.log('startDate before endDate :)');
            return false;
        }
    }
    $scope.returnNowDate = function() {
        //angular strap will not accept dynamic dates for start/end dates
        //nowdate will be today no matter what it is assigned to
        datedate = new Date();
        return datedate;
    }
}
