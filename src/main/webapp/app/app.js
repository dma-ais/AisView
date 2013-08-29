var app = angular.module('aisHDD', []);

app.factory('UrlService', function() {
    return {
        area: null,
        ids:null,
        bases:null,
        countries: null,
        types: null,
        format: null,
        tables: null,
        separator: null,
        header: null,
        fromDate: null,
        toDate: null,
        samples: null,
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
        setFormat: function(msg){
            this.format = msg;
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
        setSamples: function(msg){
            this.samples = msg;
        }
    }
});





