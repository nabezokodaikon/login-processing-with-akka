package login

import com.typesafe.scalalogging.LazyLogging

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpEntity

// import com.softwaremill.session.{
// InMemoryRefreshTokenStorage,
// SessionConfig,
// SessionManager,
// SessionSerializer,
// SingleValueSessionSerializer,
// SessionUtil
// }

// import ServerApi._

class ServerApiSpec()
  extends WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ScalatestRouteTest
  with LazyLogging {

  val api = new ServerApi(system)

  val current = FileUtil.currentDirectory

  def readFile(file: String): HttpEntity = {
    val contentType = FileUtil.getContentType(file)
    val text = FileUtil.readBinary(file)
    HttpEntity(contentType, text)
  }

  "The public route" should {

    "ルートパスにpublic/index.htmlを返す" in {
      Get() ~> api.publicRoute ~> check {
        val file = s"${current}/contents/public/index.html"
        responseAs[HttpEntity] shouldEqual readFile(file)
      }
    }

    "ログインパスにlogin.htmlを返す" in {
      Get("/login") ~> api.loginRoute ~> check {
        val file = s"${current}/contents/public/login.html"
        responseAs[HttpEntity] shouldEqual readFile(file)
      }
    }

  }

}
