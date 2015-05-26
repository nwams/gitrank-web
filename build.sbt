name := """gitrank-web"""

organization := "gitlinks"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies += "org.anormcypher" %% "anormcypher" % "0.6.0"

libraryDependencies += "org.webjars" % "bootstrap" % "3.3.4"

libraryDependencies += "org.webjars" % "angularjs" % "1.3.15"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)
