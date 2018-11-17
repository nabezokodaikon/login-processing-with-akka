package login

// import com.typesafe.scalalogging.LazyLogging

// object Main extends App with LazyLogging {

  // def helloWorld(name: String): String = {
    // "Hello " + name + "!"
  // }

  // logger.info(helloWorld("nabezokodaikokn"))
// }

import com.typesafe.config.{ ConfigFactory }
import akka.actor.ActorSystem
import akka.event.Logging

object Main extends App {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("login")
  // // bindAndHandleは暗黙のExecutionContextが必要
  // implicit val executionContext = system.dispatcher

  val log =  Logging(system.eventStream, "application")
  log.debug(s"host: ${host}, port: ${port}")

  // val api = new RestApi(system, requestTimeout(config)).routes // the RestApi provides a Route

  // implicit val materializer = ActorMaterializer()
  // val bindingFuture: Future[ServerBinding] =
    // Http().bindAndHandle(api, host, port) // HTTPサーバーの起動

  // val log =  Logging(system.eventStream, "go-ticks")
  // bindingFuture.map { serverBinding =>
    // log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  // }.onComplete {
    // case Success(_) =>
      // log.info("Success to bind to {}:{}", host, port)
    // case Failure(ex) =>
      // log.error(ex, "Failed to bind to {}:{}!", host, port)
      // system.terminate()
  // }
}
