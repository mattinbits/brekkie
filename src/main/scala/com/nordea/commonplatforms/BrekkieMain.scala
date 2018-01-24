package com.nordea.commonplatforms

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}

// $COVERAGE-OFF$ Will be covered during integration testing
object BrekkieMain extends ServiceRoute with LazyLogging {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    DAO.createWithFlyway(config) match {

      case Failure(ex) =>
        logger.error("Failed to initialize database", ex)
        throw ex

      case Success(dao) =>
        runHttp(dao, config)
    }
  }

  def now = LocalDate.now()

  def runHttp(dao: DAO, config: Config): Try[Unit] = Try{
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val networkConfig = config.getConfig("brekkie.net")
    val host = networkConfig.getString("host")
    val port = networkConfig.getInt("port")
    Http().bindAndHandle(route(dao), "0.0.0.0", 8080)
  }
}
// $COVERAGE-ON$
