package login

import com.typesafe.scalalogging.LazyLogging

import akka.actor.{ Actor, Props }
import akka.actor.ActorLogging
import akka.actor.ActorSystem

import akka.http.scaladsl.model.{
  HttpEntity,
  StatusCodes
}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

class ServerApi(system: ActorSystem)
  extends ServerRoutes {
  implicit def executionContext = system.dispatcher

  // Shutdown example
  def exampleActor = system.actorOf(Props(new ExampleActor), "Example")
  exampleActor ! "msg"
}

trait ServerRoutes
  extends LazyLogging {

  private val publicDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/content/public"
  }

  private val privateDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/content/private"
  }

  def routes: Route =
    publicIndexRoute ~
      loginRoute ~
      privateIndexRoute

  def publicIndexRoute =
    pathSingleSlash {
      get {
        val file = s"${publicDirectory}/index.html"
        val contentType = FileUtil.getContentType(file)
        val text = FileUtil.readBinary(file)
        complete(HttpEntity(contentType, text))
      }
    }

  def loginRoute =
    path("login") {
      get {
        val file = s"${publicDirectory}/login.html"
        val contentType = FileUtil.getContentType(file)
        val text = FileUtil.readBinary(file)
        complete(HttpEntity(contentType, text))
      }
    } ~
      path("loginProcess") {
        post {
          formFields(("userId", "password", "isRememberMe".?)) {
            (userId, password, isRememberMe) =>
              println(s"userId: ${userId}, password: ${password}, isRememberMe: ${isRememberMe}")
              complete(StatusCodes.OK)
          }
        }
      }

  def privateIndexRoute =
    path("member") {
      get {
        val file = s"${privateDirectory}/index.html"
        val contentType = FileUtil.getContentType(file)
        val text = FileUtil.readBinary(file)
        complete(HttpEntity(contentType, text))
      }
    }

}

// Shutdown example
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
