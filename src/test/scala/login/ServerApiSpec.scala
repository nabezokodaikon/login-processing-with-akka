package login

import com.typesafe.scalalogging.LazyLogging

import org.scalatest.{
  BeforeAndAfterAll,
  Matchers,
  WordSpecLike
}

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.{
  FormData,
  HttpEntity,
  StatusCodes
}

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

    "ログインパスにpublic/login.htmlを返す" in {
      Get("/login") ~> api.loginRoute ~> check {
        val file = s"${current}/contents/public/login.html"
        responseAs[HttpEntity] shouldEqual readFile(file)
      }
    }

    "ログアウトパスにルートパスをリダイレクトする" in {
      Get("/logout") ~> api.loginRoute ~> check {
        status shouldEqual StatusCodes.SeeOther
        responseAs[String] shouldEqual
          """The response to the request can be found under <a href="/">this URI</a> using a GET method."""
      }
    }

    "ログイン時、存在しないユーザーかつ、リンク元URIがルートパスの場合、loginページをリダイレクトする" in {
      Post("/doLogin", FormData("userId" -> "aaa", "password" -> "111", "isRememberMe" -> "on", "referrer" -> "/")) ~>
        api.loginRoute ~> check {
          status shouldEqual StatusCodes.SeeOther
          responseAs[String] shouldEqual
            """The response to the request can be found under <a href="/login">this URI</a> using a GET method."""
        }
    }

    "ログイン時、無効なパスワードかつ、リンク元URIがルートパスの場合、loginページをリダイレクトする" in {
      Post("/doLogin", FormData("userId" -> "admin", "password" -> "aaa", "isRememberMe" -> "on", "referrer" -> "/")) ~>
        api.loginRoute ~> check {
          status shouldEqual StatusCodes.SeeOther
          responseAs[String] shouldEqual
            """The response to the request can be found under <a href="/login">this URI</a> using a GET method."""
        }
    }

    "ログイン時、存在しないユーザーかつ、リンク元URIがルートパス以外の場合、loginページをリダイレクトする" in {
      Post(
        "/doLogin",
        FormData("userId" -> "aaa", "password" -> "111", "isRememberMe" -> "on", "referrer" -> "/contents/001.html")
      ) ~>
        api.loginRoute ~> check {
          status shouldEqual StatusCodes.SeeOther
          responseAs[String] shouldEqual
            """The response to the request can be found under <a href="/contents/001.html">this URI</a> using a GET method."""
        }
    }
  }

}
