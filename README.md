AisView
=======

AisViewer allows aggregation and publishing of data from both real time and historic AIS data sources.


Getting started
=======
Compile the code from the root of the project
> mvn clean install

Start up AisView by specifying the AisSources to listen to, for example the freely available hd-sf.com:9009
> java -jar target/ais-view-0.1-SNAPSHOT.jar free=hd-sf.com:9009


Streaming
=======
At any point all messages that is received by AisView by all datasources can be strea

> curl localhost:8090/stream 