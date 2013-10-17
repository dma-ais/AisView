//
// Data for url-builder
//

//Domain for request url
var requestDomain = 'http://www.example.com/';



//
// Data for source filtering section
//

//Source Tab Headings
var tabHeadings=['Source ID','Source Base Station','Source Country','Source Type','Source Region'];

//Source Ids
var sourceIds = [
    {text:'All', value: 'all&', include:true},
    {text:'Source1', value: 'src1', include:false},
    {text:'Source2', value: 'src2', include:false},
    {text:'Source3', value: 'src3', include:false},
    {text:'Source4', value: 'src4', include:false},
    {text:'Source5', value: 'src5', include:false},
    {text:'Source6', value: 'src6', include:false}];

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
var startDatepicker = {date: new Date("2012-09-01T00:00:00.000Z")};
var endDatepicker = {date: new Date("2012-09-10T00:00:00.000Z")};

//Start and end time of time-picker
var startTimepicker = {time: "00:00"};
var endTimepicker = {time: "00:00"};

//Time zone to start the time zone picker with (should be the same as on of the objects in timeZones
var timeZone = {ID: 'utc', Title: 'UTC'};

//
// Data for output format section
//

//List of available separators
var tableSeparators = [
    {ID: 'colon', Title: 'Colon', Value:':'},
    {ID: 'semi-colon', Title: 'Semicolon', Value:';'},
    {ID: 'comma', Title: 'Comma', Value:','},
    {ID: 'tab', Title: 'Tab', Value:'   '}
];

//Current separator (must be one of those in tableSeparators array)
var tableSeparator = {sep: 'colon'};

//Array of objects not currently included in query
//Extra entries can be added/removed
//id must be 0,1,2,3....x
//value-, queryName-, ex-values can be changed
//category values can NOT be changed for now TODO: make category values changeable eg in output-format-dir l 47
var notIncluded = [
    { 'id': 0,	value: "Longitude",     queryName: "longitude",     ex: "12.369",       category: "all,xyz"},
    { 'id': 1, 	value: "Latitude",		queryName: "latitude",      ex: "55.634",       category: "all,xyz"},
    { 'id': 2, 	value: "Height",		queryName: "height",        ex: "22.12",        category: "all,xyz"},
    { 'id': 3, 	value: "Timeformat1",	queryName: "timeformat1",   ex: "YYYY-MM-DD",   category: "all,time"},
    { 'id': 4, 	value: "Timeformat2",	queryName: "timeformat2",   ex: "YYYYMMDD",     category: "all,time"},
    { 'id': 5, 	value: "Timeformat3",	queryName: "timeformat3",   ex: "DD-MM-YYYY",   category: "all,time"},
    { 'id': 6, 	value: "Signal1",		queryName: "signal1",       ex: "alpha",        category: "all,signal"},
    { 'id': 7, 	value: "Signal2",		queryName: "signal2",       ex: "beta",         category: "all,signal"},
    { 'id': 8, 	value: "Signal3",		queryName: "signal3",       ex: "charlie",      category: "all,signal"},
    { 'id': 9, 	value: "Signal4",		queryName: "signal4",       ex: "delta",        category: "all,signal"}];








