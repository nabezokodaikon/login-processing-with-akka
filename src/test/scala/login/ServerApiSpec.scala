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
  HttpCookie,
  `Set-Cookie`
}

class ServerApiSpec()
  extends WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ScalatestRouteTest
  with LazyLogging {

  class TestUsingCookies(api: ServerApi) {
    val sessionCookieName = api.sessionConfig.sessionCookieConfig.name
    val refreshTokenCookieName = api.sessionConfig.refreshTokenCookieConfig.name

    def cookiesMap: Map[String, HttpCookie] =
      headers.collect { case `Set-Cookie`(cookie) => cookie.name -> cookie }.toMap

    def getSession = cookiesMap.get(sessionCookieName).map(_.value)
    def setSessionHeader(s: String) = Cookie(sessionCookieName, s)
  }

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

        Post("/doLogin", FormData("userId" -> "admin", "password" -> "", "isRememberMe" -> "false", "referrer" -> "/")) ~>
          addHeader(Cookie(api.sessionConfig.csrfCookieConfig.name, csrfCookie.cookie.value)) ~>
          addHeader(api.sessionConfig.csrfSubmittedName, csrfCookie.cookie.value) ~>
          api.loginRoute ~>
          check {
            status shouldEqual StatusCodes.SeeOther
            responseAs[String] shouldEqual
              """The response to the request can be found under <a href="/login">this URI</a> using a GET method."""
          }
      }

    }

    "ログインに成功し、リンク元URIがルートパスの場合、メンバートップページをリダイレクトする" in {
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

    "ログインに失敗し、リンク元URIがルートパス以外の場合、リンク元をリダイレクトする" in {
      Get("/login") ~> api.loginRoute ~> check {

        val file = s"${current}/contents/public/login.html"
        responseAs[HttpEntity] shouldEqual readFile(file)

        val Some(csrfCookie) = header[`Set-Cookie`]

        Post("/doLogin", FormData("userId" -> "admin", "password" -> "", "isRememberMe" -> "false", "referrer" -> "/members/003.html")) ~>
          addHeader(Cookie(api.sessionConfig.csrfCookieConfig.name, csrfCookie.cookie.value)) ~>
          addHeader(api.sessionConfig.csrfSubmittedName, csrfCookie.cookie.value) ~>
          api.loginRoute ~>
          check {
            status shouldEqual StatusCodes.SeeOther
            responseAs[String] shouldEqual
              """The response to the request can be found under <a href="/members/003.html">this URI</a> using a GET method."""
          }
      }

    }

    "ログインに成功し、リンク元URIがルートパス以外の場合、リンク元をリダイレクトする" in {
      Get("/login") ~> api.loginRoute ~> check {

        val file = s"${current}/contents/public/login.html"
        responseAs[HttpEntity] shouldEqual readFile(file)

        val Some(csrfCookie) = header[`Set-Cookie`]

        Post("/doLogin", FormData("userId" -> "admin", "password" -> "111", "isRememberMe" -> "true", "referrer" -> "/members/003.html")) ~>
          addHeader(Cookie(api.sessionConfig.csrfCookieConfig.name, csrfCookie.cookie.value)) ~>
          addHeader(api.sessionConfig.csrfSubmittedName, csrfCookie.cookie.value) ~>
          api.loginRoute ~>
          check {
            status shouldEqual StatusCodes.SeeOther
            responseAs[String] shouldEqual
              """The response to the request can be found under <a href="/members/003.html">this URI</a> using a GET method."""
          }
      }

    }

    "ログインしていない場合、ユーザー名'Guest'を取得する" in {
      Get("/getUser") ~> api.loginRoute ~> check {
        responseAs[String] shouldEqual "Guest"
      }
    }

    "ログインしている場合、そのユーザー名を取得する" in {
      Get("/login") ~> api.loginRoute ~> check {

        val Some(csrfCookie) = header[`Set-Cookie`]

        Post("/doLogin", FormData("userId" -> "admin", "password" -> "111", "isRememberMe" -> "true", "referrer" -> "/")) ~>
          addHeader(Cookie(api.sessionConfig.csrfCookieConfig.name, csrfCookie.cookie.value)) ~>
          addHeader(api.sessionConfig.csrfSubmittedName, csrfCookie.cookie.value) ~>
          api.loginRoute ~>
          check {

            val using = new TestUsingCookies(api)
            val Some(s) = using.getSession

            Get("/getUser") ~>
              addHeader(using.setSessionHeader(s)) ~>
              api.loginRoute ~>
              check {
                responseAs[String] shouldEqual "admin"
              }
          }
      }
    }

  }

}
