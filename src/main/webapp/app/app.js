var app = angular.module('aisHDD', ['$strap.directives']);

//idea from http://jsfiddle.net/sebmade/swfjT/
app.directive('ngBlur', function() {
    return function( scope, elem, attrs ) {
        elem.bind('blur', function() {
            scope.$apply(attrs.ngBlur);
        });
    };
});

//autocomplete textfields in source sections
app.directive('autoComplete', function($timeout) {
    return function(scope, iElement, iAttrs) {
        iElement.autocomplete({
            source: scope[iAttrs.uiItems],
            select: function() {
                $timeout(function() {
                    iElement.trigger('input');
                }, 0);
            }
        });
    };
});

app.factory('UrlService', function() {
    return {
        area: null,
        fromDate: null,
        toDate: null,
        timeZone: null,
        tables: null,
        separator: null,
        header: null,
        samples: null,
        sourceFiltering: null,
        mmsi: null,
        areaValidService: null,
        timeValidService: null,
        setArea: function(msg) {
            this.area = msg;
        },
        setFromDate: function(msg){
            this.fromDate = msg;
        },
        setToDate: function(msg){
            this.toDate = msg;
        },
        setTimeZone: function(msg){
            this.timeZone = msg;
        },
        setTables: function(msg){
            this.tables = msg;
        },
        setSeparator: function(msg){
            this.separator = msg;
        },
        setHeader: function(msg){
            this.header = msg;
        },
        setSamples: function(msg){
            this.samples = msg;
        },
        setSourceFiltering: function(msg){
            this.sourceFiltering = msg;
        },
        setMmsi: function(msg){
            this.mmsi = msg;
        },
        setAreaValidService: function(msg){
            this.areaValidService = msg;
        },
        setTimeValidService: function(msg){
            this.timeValidService = msg;
        }
    }
});





