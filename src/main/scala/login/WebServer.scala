package login

import scala.concurrent.duration._
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
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._

import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._

case class UserSession(userId: String)

object UserSession {
  implicit def serializer: SessionSerializer[UserSession, String] =
    new SingleValueSessionSerializer(_.userId, (userId: String) => Try { UserSession(userId) })
}

object ServerApi {

  val publicDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/contents/public"
  }

  val privateDirectory = {
    val current = FileUtil.currentDirectory
    s"${current}/contents/private"
  }

  def createResponse(file: String): ToResponseMarshallable = {
    val contentType = FileUtil.getContentType(file)
    val text = FileUtil.readBinary(file)
    HttpEntity(contentType, text)
  }

  def createResponse(dir: String, segmentsList: List[String]): ToResponseMarshallable = {
    val segments = segmentsList.mkString("/")
    val path = s"${dir}/${segments}"
    path match {
      case f if FileUtil.exists(f) => createResponse(f)
      case _ => StatusCodes.NotFound
    }
  }

  def existsContent(dir: String, segmentsList: List[String]): Boolean = {
    val segments = segmentsList.mkString("/")
    val path = s"${dir}/${segments}"
    FileUtil.exists(path)
  }

}

class ServerApi(system: ActorSystem)
  extends LazyLogging {

  import ServerApi._

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

  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret())
  implicit val sessionManager = new SessionManager[UserSession](sessionConfig)

  // ブラウザ終了後もセッションを維持する場合。
  implicit val refreshTokenStorage = new InMemoryRefreshTokenStorage[UserSession] {
    def log(msg: String) = logger.info(msg)
  }

  // private val userRequiredSession = requiredSession(refreshable, usingCookies)
  private val userInvalidateSession = invalidateSession(refreshable, usingCookies)

  def routes: Route =
    publicRoute ~
      privateRoute ~
      loginRoute

  def publicRoute =
    pathSingleSlash {
      get {
        val file = s"${publicDirectory}/index.html"
        complete(createResponse(file))
      }
    } ~
      pathPrefix("contents") {
        path(Segments) { segments: List[String] =>
          get {
            complete(createResponse(publicDirectory, segments))
          }
        }
      }

  def privateRoute =
    pathPrefix("members") {
      path(Segments) { segments: List[String] =>
        get {
          session(refreshable, usingCookies) { sessionResult =>
            sessionResult match {
              case Decoded(_) if existsContent(privateDirectory, segments) =>
                complete(createResponse(privateDirectory, segments))
              case DecodedLegacy(_) if existsContent(privateDirectory, segments) =>
                complete(createResponse(privateDirectory, segments))
              case CreatedFromToken(_) if existsContent(privateDirectory, segments) =>
                complete(createResponse(privateDirectory, segments))
              case _ if existsContent(privateDirectory, segments) =>
                val file = s"${publicDirectory}/login.html"
                complete(createResponse(file))
              case _ =>
                complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }

  def loginRoute =
    randomTokenCsrfProtection(checkHeader) {
      path("login") {
        get {
          val file = s"${publicDirectory}/login.html"
          complete(createResponse(file))
        }
      } ~
        path("logout") {
          get {
            userInvalidateSession {
              redirect("/", StatusCodes.SeeOther)
            }
          }
        } ~
        toStrictEntity(3.seconds) {
          path("doLogin") {
            post {
              formFields(("userId", "password", "isRememberMe".as[Boolean], "referrer")) {
                (userId, password, isRememberMe, referrer) =>
                  if (exampleUsers.contains(ExampleUser(userId, password))) {

                    val sessionContinuity = if (isRememberMe) refreshable else oneOff

                    setSession(sessionContinuity, usingCookies, UserSession(userId)) {
                      setNewCsrfToken(checkHeader) {
                        referrer match {
                          case ref if ref == "/" =>
                            redirect("/members/index.html", StatusCodes.SeeOther)
                          case _ =>
                            redirect(referrer, StatusCodes.SeeOther)
                        }
                      }
                    }

                  } else {
                    referrer match {
                      case ref if ref == "/" =>
                        redirect("/login", StatusCodes.SeeOther)
                      case _ =>
                        redirect(referrer, StatusCodes.SeeOther)
                    }
                  }
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
                  complete("Guest")
              }
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
