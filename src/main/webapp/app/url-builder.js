function urlBuilder($scope,UrlService) {

    $scope.url = function(){
        var base = 'http://www.example.com/';

        var requestString = base+
            UrlService.ids+
            UrlService.bases+
            UrlService.countries+
            UrlService.types+
            UrlService.area+
            UrlService.fromDate+
            UrlService.toDate+
            UrlService.tables+
            UrlService.separator+
            UrlService.header+
            UrlService.samples;

        if (requestString.slice(-1)=='&') requestString=requestString.slice(0,-1);
        return requestString;
    }
}
