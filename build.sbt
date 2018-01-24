import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.nordea.commonplatforms",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT",
      fork in Test := true,
      fork in run := true,
      cancelable in Global := true
    )),
    name := "brekkie",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      flyway,
      h2,
      lazyLogging,
      sl4jLog4j,
      slick,
      slickHikari,
      akkaHttp,
      akkaHttpTestkit
    ),
    dockerExposedPorts := Seq(8080)
  ).enablePlugins(SbtTwirl).enablePlugins(DockerPlugin, JavaAppPackaging)
