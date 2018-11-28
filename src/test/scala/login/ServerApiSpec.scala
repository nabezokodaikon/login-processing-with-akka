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
import akka.http.scaladsl.model.headers.{
  Cookie,
  `Set-Cookie`
}

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

  }

  "The login route" should {
    
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

    "ログインに失敗し、リンク元URIがルートパスの場合、ログインページをリダイレクトする" in {
      Get("/login") ~> api.loginRoute ~> check {

        val file = s"${current}/contents/public/login.html"
        responseAs[HttpEntity] shouldEqual readFile(file)

        val Some(csrfCookie) = header[`Set-Cookie`]

        Post("/doLogin", FormData("userId" -> "admin", "password" -> "111", "isRememberMe" -> "false", "referrer" -> "/")) ~>
          addHeader(Cookie(api.sessionConfig.csrfCookieConfig.name, csrfCookie.cookie.value)) ~>
          addHeader(api.sessionConfig.csrfSubmittedName, csrfCookie.cookie.value) ~>
          api.loginRoute ~>
          check {
            status shouldEqual StatusCodes.SeeOther
            responseAs[String] shouldEqual
              """The response to the request can be found under <a href="/members/index.html">this URI</a> using a GET method."""
          }
      }

    }

    // "ログイン時、ログインに失敗し、リンク元URIがルートパス以外の場合、loginページをリダイレクトする" in {
    // Post(
    // "/doLogin",
    // FormData("userId" -> "aaa", "password" -> "111", "isRememberMe" -> "on", "referrer" -> "/contents/001.html")
    // ) ~>
    // api.loginRoute ~> check {
    // status shouldEqual StatusCodes.SeeOther
    // responseAs[String] shouldEqual
    // """The response to the request can be found under <a href="/contents/001.html">this URI</a> using a GET method."""
    // }
    // }

  }

}
