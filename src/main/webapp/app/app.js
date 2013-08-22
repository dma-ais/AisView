var app = angular.module('aisHDD', ['$strap.directives']);

app.controller('dateTimePicker', function($scope, $window, $location) {

    // Datepicker directive
    $scope.startDatepicker = {date: ''};
    $scope.endDatepicker = {date: ''};

    $scope.startTimepicker = {time: ''};
    $scope.endTimepicker = {time: ''};


    ////TODO: change to service instead of pushing to root scope
    $scope.$root.startDatepickerInRoot = $scope.startDatepicker;
    $scope.$root.endDatepickerInRoot = $scope.endDatepicker;

    $scope.$root.startTimepickerInRoot = $scope.startTimepicker;
    $scope.$root.endTimepickerInRoot = $scope.endTimepicker;






});
