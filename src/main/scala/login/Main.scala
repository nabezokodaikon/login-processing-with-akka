package login

import com.typesafe.config.{ ConfigFactory }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import scala.util.{ Failure, Success }

object Main extends App {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("login")
  // // bindAndHandleは暗黙のExecutionContextが必要
  implicit val executionContext = system.dispatcher

  val log = Logging(system.eventStream, "application")
  log.debug(s"host: ${host}, port: ${port}")

  val api = new ServerApi(system).routes

  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port)

  bindingFuture.map { serverBinding =>
    log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onComplete {
    case Success(_) =>
      log.info(s"Success to bind to ${host}:${port}")
    case Failure(ex) =>
      log.error(ex, s"Failed to bind to ${host}:${port}!")
      system.terminate()
  }

  import scala.concurrent.Await
  import scala.concurrent.duration._
  import akka.actor.CoordinatedShutdown
  // CoordinatedShutdown(system).addTask(
  // CoordinatedShutdown.PhaseBeforeServiceUnbind, "app-shutdown"
  // ) { () =>
  // // killSwitch.shutdown()
  // system.terminate()
  // Await.result(bindingFuture, Duration.Inf)
  // Future.successful(akka.Done)
  // }

  CoordinatedShutdown(system).addJvmShutdownHook {
    system.terminate()
    Await.result(bindingFuture, Duration.Inf)
  }

}
