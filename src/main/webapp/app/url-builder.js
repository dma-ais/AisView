function urlBuilder($scope,UrlService) {

    $scope.url = function(){
        var base = requestDomain;

        var requestString = base+
            UrlService.ids+
            UrlService.bases+
            UrlService.countries+
            UrlService.types+
            UrlService.area+
            UrlService.fromDate+
            UrlService.toDate+
            UrlService.timeZone+
            UrlService.tables+
            UrlService.separator+
            UrlService.header+
            UrlService.samples;

        if (requestString.slice(-1)=='&') requestString=requestString.slice(0,-1);
        return requestString;
    }
}
