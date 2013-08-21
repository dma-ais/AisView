var app = angular.module('aisHDD', ['$strap.directives']);

app.controller('MainCtrl', function($scope, $window, $location) {

    // Datepicker directive
    $scope.startDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};

    $scope.endDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};


    $scope.starTimepicker;

    $scope.endTimepicker;


});
