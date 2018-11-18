package login

import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpCharsets,
  MediaTypes
}
import akka.http.scaladsl.model.ContentType.WithCharset

import java.io.{ File, FileNotFoundException }
import scala.io.Source
import scala.util.control.Exception.catching

import Loan.using

object FileUtil {

  private val enc = "UTF-8"

  val currentDirectory: String =
    new File(".").getAbsoluteFile().getParent()

  def exists(name: String): Boolean =
    new File(name).exists()

  def readText(name: String): String = {
    catching(classOf[FileNotFoundException]).either {
      using(Source.fromFile(name, enc)) { src =>
        src.mkString
      }
    } match {
      case Right(text) => text
      case Left(e) => e.getMessage
    }
  }

  private def getExtension(file: String): String = {
    file.split('.').last
  }

  def getContentType(file: String): ContentType = {
    getExtension(file) match {
      case "html" => ContentTypes.`text/html(UTF-8)`
      case "css" => WithCharset(MediaTypes.`text/css`, HttpCharsets.`UTF-8`)
      case "js" => WithCharset(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`)
      case "svg" => MediaTypes.`image/svg+xml`
      case "png" => MediaTypes.`image/png`
      case "jpg" => MediaTypes.`image/jpeg`
      case "ico" => MediaTypes.`image/x-icon`
      case _ => WithCharset(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`)
    }
  }
}
