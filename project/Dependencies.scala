import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val flyway = "org.flywaydb" % "flyway-core" % "4.2.0"
  lazy val h2 = "com.h2database" % "h2" % "1.3.148"
  lazy val lazyLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  lazy val sl4jLog4j = "org.slf4j" % "slf4j-log4j12" % "1.7.25"

  lazy val slick = "com.typesafe.slick" %% "slick" % "3.2.1"
  lazy val slickHikari = "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1"

  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.11"
  lazy val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test
}
