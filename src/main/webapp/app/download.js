function download($scope,UrlService) {

    //Control of number of samples
    $scope.samples = '';

    $scope.$watch('samples', function(){

        var samples = '';
        //build string of samples
        if($scope.samples=='') samples='';
        else samples='samples='+$scope.samples+'&';

        //Send to service
        UrlService.setSamples(samples);
    });

}