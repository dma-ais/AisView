var app = angular.module('aisHDD', ['$strap.directives']);

app.controller('dateTimePicker', function($scope, $window, $location) {

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

    ////TODO: change to service instead of pushing to root scope
    $scope.$root.startDatepickerInRoot = $scope.startDatepicker;
    $scope.$root.endDatepickerInRoot = $scope.endDatepicker;

    $scope.$root.startTimepickerInRoot = $scope.startTimepicker;
    $scope.$root.endTimepickerInRoot = $scope.endTimepicker;

    $scope.$root.startTimeZoneInRoot = $scope.startTimeZone;
    $scope.$root.endTimeZoneInRoot = $scope.endTimeZone;





});

var INTEGER_REGEXP = /^\-?\d*$/;
app.directive('integer', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$parsers.unshift(function(viewValue) {
                if (INTEGER_REGEXP.test(viewValue)) {
                    // it is valid
                    ctrl.$setValidity('integer', true);
                    return viewValue;
                } else {
                    // it is invalid, return undefined (no model update)
                    ctrl.$setValidity('integer', false);
                    return undefined;
                }
            });
        }
    };
});
