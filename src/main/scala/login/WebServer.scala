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
import akka.http.scaladsl.marshalling.ToResponseMarshallable
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

  // private val userRequiredSession = requiredSession(refreshable, usingCookies)
  private val userInvalidateSession = invalidateSession(refreshable, usingCookies)

  private val publicDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/contents/public"
  }

  private val privateDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/contents/private"
  }

  private def createResponse(file: String): ToResponseMarshallable = {
    val contentType = FileUtil.getContentType(file)
    val text = FileUtil.readBinary(file)
    HttpEntity(contentType, text)
  }

  private def createResponse(dir: String, segmentsList: List[String]): ToResponseMarshallable = {
    val segments = segmentsList.mkString("/")
    val path = s"${dir}/dir/${segments}"
    path match {
      case f if FileUtil.exists(f) => createResponse(f)
      case _ => StatusCodes.NotFound
    }
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
              complete(createResponse(file))
            case DecodedLegacy(session) =>
              println(session)
              val file = s"${privateDirectory}/index.html"
              complete(createResponse(file))
            case CreatedFromToken(session) =>
              println(session)
              val file = s"${privateDirectory}/index.html"
              complete(createResponse(file))
            case _ =>
              val file = s"${publicDirectory}/index.html"
              complete(createResponse(file))
          }
        }
      }
    } ~
      pathPrefix("contents") {
        path(Segments) { segments: List[String] =>
          get {
            session(refreshable, usingCookies) { sessionResult =>
              sessionResult match {
                case Decoded(session) =>
                  println(session)
                  complete(createResponse(privateDirectory, segments))
                case DecodedLegacy(session) =>
                  println(session)
                  complete(createResponse(privateDirectory, segments))
                case CreatedFromToken(session) =>
                  println(session)
                  complete(createResponse(privateDirectory, segments))
                case _ =>
                  complete(createResponse(publicDirectory, segments))
              }
            }
          }
        }
      }

  private def loginRoute =
    path("login") {
      get {
        val file = s"${publicDirectory}/login.html"
        complete(createResponse(file))
      }
    } ~
      path("doLogin") {
        post {
          formFields(('userId.as[String], 'password.as[String], 'isRememberMe.?)) {
            (userId, password, isRememberMe) =>
              println(s"userId: ${userId}, password: ${password}, isRememberMe: ${isRememberMe}")
              if (exampleUsers.contains(ExampleUser(userId, password))) {
                val sessionContinuity = isRememberMe match {
                  case Some(_) => refreshable
                  case None => oneOff
                }
                setSession(sessionContinuity, usingCookies, UserSession(userId)) {
                  redirect("/", StatusCodes.SeeOther)
                }
              } else {
                redirect("login", StatusCodes.SeeOther)
              }
          }
        }
      } ~
      path("getUser") {
        get {
          session(refreshable, usingCookies) { sessionResult =>
            sessionResult match {
              case Decoded(session) =>
                complete(session.userId)
              case DecodedLegacy(session) =>
                complete(session.userId)
              case CreatedFromToken(session) =>
                complete(session.userId)
              case _ =>
                complete("unknown")
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
