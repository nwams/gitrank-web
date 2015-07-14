name := """gitrank-web"""

organization := "gitlinks"

version := "1.0-SNAPSHOT"

maintainer := "Nicolas Joseph <nicolas@nicolasjoseph.com>"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "com.mohiva" %% "play-silhouette" % "3.0.0-RC1",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0-RC1" % "test",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "bootswatch-paper" % "3.3.1+2",
  "org.webjars.bower" % "octicons" % "2.2.3",
  specs2 % Test,
  cache,
  filters,
  ws
)

scalacOptions in Test ++= Seq("-Yrangepos")

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

enablePlugins(JavaServerAppPackaging)

dockerBaseImage := "colisweb/debian-oracle-java8"

dockerRepository := Some("gitlinks")

dockerUpdateLatest := true
