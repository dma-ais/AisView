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
        ids:null,
        bases:null,
        countries: null,
        types: null,
        regions: null,
        area: null,
        fromDate: null,
        toDate: null,
        timeZone: null,
        tables: null,
        separator: null,
        header: null,
        samples: null,
        allInputValidService: null,
        setArea: function(msg) {
            this.area = msg;
        },
        setIds: function(msg) {
            this.ids = msg;
        },
        setBases: function(msg) {
            this.bases = msg;
        },
        setCountries: function(msg){
            this.countries = msg;
        },
        setTypes: function(msg){
            this.types = msg;
        },
        setRegions: function(msg){
            this.regions = msg;
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
        setFromDate: function(msg){
            this.fromDate = msg;
        },
        setToDate: function(msg){
            this.toDate = msg;
        },
        setTimeZone: function(msg){
            this.timeZone = msg;
        },
        setSamples: function(msg){
            this.samples = msg;
        },
        setAllInputValidService: function(msg){
            this.allInputValidService = msg;
        }
    }
});





