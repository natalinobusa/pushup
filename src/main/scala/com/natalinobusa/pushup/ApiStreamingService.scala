package com.natalinobusa.pushup

// the service, actors and paths

import akka.actor._

import spray.routing.{RequestContext, HttpService}
import spray.can.Http
import spray.util._
import spray.http._
import MediaTypes._

// akka eventbus
import akka.event.{ActorEventBus, LookupClassification}

case class MessageEvent(val channel: String, val message: Any)

// Messages sent via the actor event loop
case object End
case class Message(text: String)

class LookupEventBus extends ActorEventBus with LookupClassification {
  type Event = MessageEvent
  type Classifier = String

  protected def mapSize(): Int = 10

  protected def classify(event: Event): Classifier = {
    event.channel
  }

  protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.message
  }
}


class ApiPushupServiceActor extends Actor with ApiPushupService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing,
  // timeout handling or alternative handler registration
  def receive = runRoute(serviceRoute)
}

// Routing embedded in the actor
trait ApiPushupService extends HttpService {

  implicit def executionContext = actorRefFactory.dispatcher

  //eventbus
  val eventBus = new LookupEventBus

  val ingestRoute = {
    pathPrefix("in") {
      pathEnd {
        post {
          entity(as[String]) { s =>
            eventBus.publish(MessageEvent("/events", Message(s)))
            complete(StatusCodes.Created, s)
          }
        }
      }
    }
  }

  val streamRoute = {
   pathPrefix("stream") {
     pathEnd {
       get {
         ctx => sendStreamingResponse(ctx)
       }
     }
   }
  }

  val renderRoute = {
    pathPrefix("dashboard") {
      pathEnd {
        getFromResource("render.html")
      }
    }
  }

  val serviceRoute = {
    pathPrefix("api") {
      streamRoute ~
      ingestRoute
    } ~
    renderRoute
  }

  def sendStreamingResponse(ctx: RequestContext): Unit = {
    val listener = actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {

          val `text/event-stream` = MediaType.custom("text/event-stream")
          MediaTypes.register(`text/event-stream`)

          // we use the successful sending of a chunk as trigger for scheduling the next chunk
          val responseStart = HttpResponse(entity = HttpEntity(`text/event-stream`, streamStart))
          ctx.responder ! ChunkedResponseStart(responseStart).withAck()

          def receive = {
            case Message(s) =>
              val nextChunk = MessageChunk("data: " + s + "\n\n")
              ctx.responder ! nextChunk.withAck()

            case End =>
              ctx.responder ! MessageChunk(streamEnd)
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)

            case ev: Http.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
              context.stop(self)

          }
        }
      }
    }
    eventBus.subscribe(listener, "/events")
  }

  // we prepend 2048 "empty" bytes to push the browser to immediately start displaying the incoming chunks
  lazy val streamStart = " " * 2048 + "\n\n"
  lazy val streamEnd = "\n\n"

}
