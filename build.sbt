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
coverageExcludedPackages := "com\\.galekseev\\.dynalist_to_anki\\.Main"

parallelExecution in Test := false

scalastyleFailOnWarning := true
(scalastyleConfig in Test) := baseDirectory.value / "scalastyle-test-config.xml"

assemblyMergeStrategy in assembly := {
  case PathList("jackson-annotations-2.10.1.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-core-2.10.1.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-databind-2.10.1.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-datatype-jdk8-2.10.1.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-datatype-jsr310-2.10.1.jar", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}