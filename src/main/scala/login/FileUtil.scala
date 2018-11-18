package login

import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpCharsets,
  MediaTypes
}
import akka.http.scaladsl.model.ContentType.WithCharset

import java.io.{
  File,
  IOException,
  BufferedInputStream,
  FileInputStream
}
import scala.util.control.Exception.catching

import Loan.using

object FileUtil {

  val currentDirectory: String =
    new File(".").getAbsoluteFile().getParent()

  def exists(name: String): Boolean =
    new File(name).exists()

  def readBinary(name: String): Array[Byte] = {
    catching(classOf[IOException]).either {
      using(new BufferedInputStream(new FileInputStream(name))) { in =>
        Stream.continually(in.read).takeWhile(!_.equals(-1)).map(_.toByte).toArray
      }
    } match {
      case Right(bin) => bin
      case Left(_) => Array[Byte]()
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
