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
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._

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
  // private val userRequiredSession = requiredSession(refreshable, usingCookies)
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

  private def createEntity(file: String) = {
    val contentType = FileUtil.getContentType(file)
    val text = FileUtil.readBinary(file)
    HttpEntity(contentType, text)
  }

  def routes: Route =
    contentsRoute ~
      loginRoute

  private def contentsRoute =
    pathSingleSlash {
      get {
        session(refreshable, usingCookies) { sessionResult =>
          sessionResult match {
            case Decoded(session) =>
              println(session)
              val file = s"${privateDirectory}/index.html"
              complete(createEntity(file))
            case DecodedLegacy(session) =>
              println(session)
              val file = s"${privateDirectory}/index.html"
              complete(createEntity(file))
            case CreatedFromToken(session) =>
              println(session)
              val file = s"${privateDirectory}/index.html"
              complete(createEntity(file))
            case _ =>
              val file = s"${publicDirectory}/index.html"
              complete(createEntity(file))
          }
        }
      }
    } ~
      pathPrefix("contents") {
        path(Segments) { x: List[String] =>
          get {
            session(refreshable, usingCookies) { sessionResult =>
              sessionResult match {
                case Decoded(session) =>
                  println(session)
                  val segments = x.mkString("/")
                  val path = s"${privateDirectory}/dir/${segments}"
                  path match {
                    case f if FileUtil.exists(f) => complete(createEntity(f))
                    case _ => complete(StatusCodes.NotFound)
                  }
                case _ =>
                  val segments = x.mkString("/")
                  val path = s"${publicDirectory}/dir/${segments}"
                  path match {
                    case f if FileUtil.exists(f) => complete(createEntity(f))
                    case _ => complete(StatusCodes.NotFound)
                  }
              }
            }
          }
        }
      }

  private def loginRoute =
    path("login") {
      get {
        val file = s"${publicDirectory}/login.html"
        complete(createEntity(file))
      }
    } ~
      path("doLogin") {
        post {
          formFields(("userId", "password", "isRememberMe".?)) {
            (userId, password, isRememberMe) =>
              println(s"userId: ${userId}, password: ${password}, isRememberMe: ${isRememberMe}")
              if (exampleUsers.contains(ExampleUser(userId, password))) {
                userSetSession(UserSession(userId)) {
                  redirect("/", StatusCodes.SeeOther)
                }
              } else {
                redirect("login", StatusCodes.SeeOther)
              }
          }
        }
      } ~
      path("logout") {
        get {
          userInvalidateSession {
            redirect("login", StatusCodes.SeeOther)
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
