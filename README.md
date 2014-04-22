AisView
=======


AisView allows aggregation and publishing of data from both real time and historic AIS data sources using REST.

AisView has 3 main functionalities:
* __Realtime Streaming__ : Streaming of incoming AIS data with easy-to-use filtering
* __Realtime Monitoring__: Reporting of latest static and positional data from vessels
* __Historical Querying__  : Historic querying of AIS data via [AisStore](https://github.com/dma-ais/AisStore "AisStore")

Getting started
=======
Compile the code from the root of the project. Maven 3.0.4 (or later) and Java 7 is needed:
> mvn clean install

Unless you already have AisStore setup. The easiest way to get started is by using a freely available AIS stream.

Start up AisView by specifying the AisSources to listen to as parameters. Here we are using the freely available hd-sf.com:9009
> java -jar target/ais-view-0.1-SNAPSHOT.jar free=hd-sf.com:9009



Realtime Streaming
=======
At any point all messages that is received by AisView can be streamed in realtime to any REST based client.
Either by curl'ing it from a command promt:

> curl localhost:8090/stream 

Or by opening the url in a browser:
Browser: http://localhost:8090/stream

The stream can be filtered in a number of ways

Realtime Monitoring
=======

Historical Querying
=======

### Area ###
An rectangular area can be specified: (top left latitude, top left longitude, bottom right latitude, buttom left latitude)
?box=1,1,4,5 

### Interval ###
Interval are specified as a ISO 8601 time interval. For example,
?interval=P1Y2M10DT2H30M/2008-05-11T15:30:00Z

### Modes ###
Two output modes are supported: Raw data or Table:
?output=raw or ?output=table

### MMSI ###
Filtering based on mmsi numbers:  
?mmsi=123456789,234567890,333444555

### Limit ###
A limit can be placed on the number of packets that should be returned:
?limit=50 (will only return 50 packets)

### Source filtering ###
All packets can be filtered based on some property on the source they are received from 
* Source Id: id
* Source Basestation: bs
* Source Country: country
* Source Type: type
* Source Region: region
?filter=id=AISD,2,3,4&region=23434


