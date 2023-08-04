ThisBuild / scalaVersion := "2.13.11"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-film-service",
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-native"  % "4.1.0-M3",
      "org.json4s" %% "json4s-jackson" % "4.1.0-M3",
//      "net.liftweb" %% "lift-webkit"    % "3.1.1" % "provided", // fixed to 2.13
      "net.liftweb" %% "lift-webkit" % "3.4.3",
//      "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.2",  // fixed to 2.13
      "com.typesafe.play" %% "play-ws-standalone-json" % "2.1.11",
//      "dev.zio"           %% "zio-test-mock"           % "1.0.12",
      "dev.zio"           %% "zio"                     % "1.0.14",
      "dev.zio"           %% "zio-test"                % "1.0.14" % Test,
      "dev.zio"           %% "zio-test-sbt"                % "1.0.14" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
