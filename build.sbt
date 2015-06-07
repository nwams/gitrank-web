name := """gitrank-web"""

organization := "gitlinks"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "angularjs" % "1.4.0",
  "org.webjars" % "bootstrap" % "3.3.4",
  javaWs,
  jdbc,
  anorm,
  cache,
  ws
)
