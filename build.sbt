name := "scala-cli-project-template"

version := "0.1"

scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

mainClass in assembly := Some("com.galekseev.Main")

val playWsStandaloneVersion = "2.1.2"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % playWsStandaloneVersion,
  "com.typesafe.play" %% "play-ws-standalone-json" % playWsStandaloneVersion,
  "com.typesafe" % "config" % "1.4.1",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",

  "org.scalatest" %% "scalatest" % "3.2.3" % Test,
  "org.scalacheck" %% "scalacheck" % "1.15.2" % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.3.0" % Test,
  "org.mockito" %% "mockito-scala-scalatest" % "1.16.25" % Test,
  "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test
)

coverageMinimum := 100
coverageFailOnMinimum := true
coverageExcludedPackages := "com\\.galekseev\\.Main"

scalastyleFailOnWarning := true
