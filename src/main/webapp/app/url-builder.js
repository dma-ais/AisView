function urlBuilder($scope,UrlService) {



    $scope.url = function(){
        var base = requestDomain;

        var requestString = base+
            UrlService.fromDate+
            UrlService.toDate+
            UrlService.ids+
            UrlService.bases+
            UrlService.countries+
            UrlService.types+
            UrlService.regions+
            UrlService.area+
            //UrlService.timeZone+
            UrlService.tables+
            UrlService.separator+
            UrlService.header+
            UrlService.samples;

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
