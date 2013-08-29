function timeSelection($scope,UrlService) {

    $scope.timeZones = [
        {ID: 'utc', Title: 'UTC'},
        {ID: 'utc/gmt+1', Title: 'UTC/GMT+1'}
    ];

    // Datepicker directive
    $scope.startDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};
    $scope.endDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};

    $scope.startTimepicker = {time: "00:00 AM"};
    $scope.endTimepicker = {time: "00:00 AM"};

    $scope.startTimeZone = {zone: 'utc'};
    $scope.endTimeZone = {zone: 'utc'};


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

}
