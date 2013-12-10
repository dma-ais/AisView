//
// Data for url-builder
//

//Domain for request url
var requestDomain = 'http://10.0.0.121:8090/store/query?';


//
//Rest-service query parameters
//

var source_restService = 'filter=';         //used in source-filters.js
var mmsi_restService = 'mmsi=';             //used in mmsi-filters.js
var area_restService = 'box=';              //used in area-selection.js
var time_restService = 'interval=';         //used in time-selection.js
var output_restService = 'output=table&';   //used in output-format-ctrl.js
var columns_restService = 'columns=';       //used in output-format-ctrl.js
var separator_restService = 'separator=';   //used in output-format-ctrl.js
var noHeader_restService = 'noHeader&';     //used in output-format-ctrl.js
var samples_restService = '&limit=';         //used in url-builder.js


//
// Data for source filtering section
//

//Source Tab Headings
var tabHeadings=['Source ID','Source Base Station','Source Country','Source Type','Source Region'];

//Source Ids
/*var sourceIds = [
    {text:'All', value: 'all&', include:true},
    {text:'Source1', value: 'src1', include:false},
    {text:'Source2', value: 'src2', include:false},
    {text:'Source3', value: 'src3', include:false},
    {text:'Source4', value: 'src4', include:false},
    {text:'Source5', value: 'src5', include:false},
    {text:'Source6', value: 'src6', include:false}];
*/

//Source Bases
var sourceBases = [{text:1+'.', input:'', counter: 1}];


//
// Data for time selection section
//

//Time Zones
var timeZones = [
    {ID: 'utc', Title: 'UTC'},
    {ID: 'utc/gmt+1', Title: 'UTC/GMT+1'},
    {ID: 'utc/gmt+2', Title: 'UTC/GMT+2'},
    {ID: 'utc/gmt+3', Title: 'UTC/GMT+3'},
    {ID: 'utc/gmt+4', Title: 'UTC/GMT+4'}
];

//Start and end date of date-picker
var startDatepicker = {date: new Date("2013-10-15T00:00:00.000Z")};
var endDatepicker = {date: new Date("2013-10-15T00:00:00.000Z")};

//Start and end time of time-picker
var startTimepicker = {time: "14:00"};
var endTimepicker = {time: "14:10"};

//Time zone to start the time zone picker with (should be the same as on of the objects in timeZones
var timeZone = {ID: 'utc', Title: 'UTC'};

//
// Data for output format section
//

//List of available separators
var tableSeparators = [
    {Title: 'Semicolon', Value:';'},
    {Title: 'Colon', Value:':'},
    {Title: 'Comma', Value:','}
];

//Current separator (must be one of those in tableSeparators array)
var tableSeparator = {sep: ';'};

//Array of objects not currently included in query
//Extra entries can be added/removed
//id must be 0,1,2,3....x
//value-, queryName-, ex-values can be changed
//category values can NOT be changed for now TODO: make category values changeable eg in output-format-dir line 47
var notIncluded = [
    { 'id': 0,	value: "Longitude",     queryName: "lon",           ex: "12.369",       category: "all,xyz"},
    { 'id': 1, 	value: "Latitude",		queryName: "lat",           ex: "55.634",       category: "all,xyz"},
    { 'id': 2, 	value: "Height",		queryName: "height",        ex: "22.12",        category: "all,xyz"},
    { 'id': 3, 	value: "Timeformat1",	queryName: "utc",           ex: "YYYY-MM-DD",   category: "all,time"},
    { 'id': 4, 	value: "Timeformat2",	queryName: "timeformat2",   ex: "YYYYMMDD",     category: "all,time"},
    { 'id': 5, 	value: "Timeformat3",	queryName: "timeformat3",   ex: "DD-MM-YYYY",   category: "all,time"},
    { 'id': 6, 	value: "Signal1",		queryName: "signal1",       ex: "alpha",        category: "all,signal"},
    { 'id': 7, 	value: "Signal2",		queryName: "signal2",       ex: "beta",         category: "all,signal"},
    { 'id': 8, 	value: "Signal3",		queryName: "signal3",       ex: "charlie",      category: "all,signal"},
    { 'id': 9, 	value: "Signal4",		queryName: "signal4",       ex: "delta",        category: "all,signal"}];








