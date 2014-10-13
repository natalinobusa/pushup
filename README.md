Push up
======

Pushing message from REST to Stream

Howto
===

The little demo pushes POST json to streaming connection

 - /api/in 
 The api sink enpoint: ingest any given json passed to it
 - /api/stream 
 This is a text/event-stream endpoint, pushing up messages according to the eventsource protocol

The demo can be viewed by looking at the url /dashboard

Demo
===

Open up a browser on localhost:8888/dashboard

POST a message to the /api/in endpoint
curl -d '{"data":3}

Watch in awe the highchart graph changing in realtime on the browser as the data is pushed via the curl command

