package login

import scala.util.Try

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

import com.softwaremill.session.{
  InMemoryRefreshTokenStorage,
  SessionConfig,
  SessionManager,
  SessionSerializer,
  SingleValueSessionSerializer,
  SessionUtil
}
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

case class UserSession(userId: String)

object UserSession {
  implicit def serializer: SessionSerializer[UserSession, String] =
    new SingleValueSessionSerializer(_.userId, (userId: String) => Try { UserSession(userId) })
}

class ServerApi(system: ActorSystem)
  extends LazyLogging {
  implicit def executionContext = system.dispatcher

  // Shutdown example
  def exampleActor = system.actorOf(Props(new ExampleActor), "Example")
  exampleActor ! "msg"

  // User data example
  case class ExampleUser(id: String, password: String)
  val exampleUsers = List(
    ExampleUser("admin", "111"),
    ExampleUser("user", "222")
  )

  private val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret())
  implicit val sessionManager = new SessionManager[UserSession](sessionConfig)

  // ブラウザ終了後もセッションを維持する場合。
  implicit val refreshTokenStorage = new InMemoryRefreshTokenStorage[UserSession] {
    def log(msg: String) = logger.info(msg)
  }

  // ブラウザ終了後もセッションを維持する場合。
  private def userSetSession(session: UserSession) = setSession(refreshable, usingCookies, session)
  private val userRequiredSession = requiredSession(refreshable, usingCookies)
  private val userInvalidateSession = invalidateSession(refreshable, usingCookies)

  // ブラウザ終了時にセッションを終了する場合。
  // private def userSetSession(session: UserSession) = setSession(oneOff, usingCookies, session)
  // private val userRequiredSession = requiredSession(oneOff, usingCookies)
  // private val userInvalidateSession = invalidateSession(oneOff, usingCookies)

  private val publicDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/content/public"
  }

  private val privateDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/content/private"
  }

  def routes: Route =
    publicRoute ~
      privateRoute

  private def publicRoute =
    pathSingleSlash {
      get {
        val file = s"${publicDirectory}/index.html"
        val contentType = FileUtil.getContentType(file)
        val text = FileUtil.readBinary(file)
        complete(HttpEntity(contentType, text))
      }
    } ~
      path("login") {
        get {
          val file = s"${publicDirectory}/login.html"
          val contentType = FileUtil.getContentType(file)
          val text = FileUtil.readBinary(file)
          complete(HttpEntity(contentType, text))
        }
      }

  private def privateRoute =
    path("doLogin") {
      post {
        formFields(("userId", "password", "isRememberMe".?)) {
          (userId, password, isRememberMe) =>
            println(s"userId: ${userId}, password: ${password}, isRememberMe: ${isRememberMe}")
            if (exampleUsers.contains(ExampleUser(userId, password))) {
              userSetSession(UserSession(userId)) {
                redirect("member", StatusCodes.SeeOther)
              }
            } else {
              redirect("login", StatusCodes.SeeOther)
            }
        }
      }
    } ~
      path("member") {
        get {
          userRequiredSession { userSession =>
            println(userSession)
            val file = s"${privateDirectory}/index.html"
            val contentType = FileUtil.getContentType(file)
            val text = FileUtil.readBinary(file)
            complete(HttpEntity(contentType, text))
          }
        }
      } ~
      path("logout") {
        get {
          userRequiredSession { userSession =>
            println(userSession)
            userInvalidateSession {
              redirect("login", StatusCodes.SeeOther)
            }
          }
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
