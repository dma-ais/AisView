function urlBuilder($scope,UrlService) {

    //control to check if all input on side is valid
    UrlService.setAreaValidService(true);
    //$scope.allInputValid = true;

    var requestString;

    $scope.url = function(){
        var base = requestDomain;
        if(UrlService.areaValidService === true && UrlService.timeValidService === true){
            requestString = base+
                UrlService.fromDate+
                UrlService.toDate+
                UrlService.sourceFiltering+
                UrlService.mmsi+
                UrlService.area+
                //UrlService.timeZone+
                UrlService.tables+
                UrlService.separator+
                UrlService.header+
                UrlService.samples;

        }
        else {

            requestString='Error in one or more input fields';
        }

        if (requestString.slice(-1)=='&') requestString=requestString.slice(0,-1);
        return requestString;
    }

    $scope.openRequestCall = function(){
        var urlString = $scope.url();
        window.open(urlString);
    }
    $scope.download = function()
    {
        var urlString = $scope.url();

        var a = document.createElement('a');
        a.href = urlString;
        a.download = 'AisStoreDump.txt'
        a.click();
    }
}
