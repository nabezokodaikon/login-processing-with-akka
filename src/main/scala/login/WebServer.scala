package login

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._

import akka.actor.{ Actor, Props }
import akka.actor.ActorLogging

class ServerApi(system: ActorSystem)
  extends ServerRoutes {
  implicit def executionContext = system.dispatcher

  def exampleActor = system.actorOf(Props(new ExampleActor), "Example")
  exampleActor ! "msg"
}

trait ServerRoutes {

  def routes: Route = eventsRoute

  def eventsRoute =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

}

class ExampleActor()
  extends Actor
  with ActorLogging {

  override def preStart() = {
    log.debug(s"ExampleActor preStart.");
  }

  override def postStop() = {
    log.debug(s"ExampleActor postStop.")
  }

  def receive = {
    case msg: String => log.debug(msg)
  }
}
