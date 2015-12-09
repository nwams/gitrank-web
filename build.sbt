import java.nio.file.{Paths, Files, StandardOpenOption}

name := """gitrank-web"""

organization := "gitlinks"

version := "1.0-SNAPSHOT"

maintainer := "Nicolas Joseph <nicolas@nicolasjoseph.com>"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "com.mohiva" %% "play-silhouette" % "3.0.4",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test",
  "org.scalaj" %% "scalaj-http" % "1.1.5", // Used for the test, to request neo without play
  "net.ceedubs" %% "ficus" % "1.1.2",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "Semantic-UI" % "2.1.4",
  "org.webjars.bower" % "lodash" % "3.10.1",
  "org.webjars.npm" % "emojify.js" % "1.1.0",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars.bower" % "octicons" % "2.2.3",
  "org.webjars" % "d3js" % "3.5.5-1",
  "com.sendgrid" % "sendgrid-java" % "2.2.2",
  "org.apache.lucene" % "lucene-queryparser" % "3.3.0",
    specs2 % Test,
  "org.specs2" %% "specs2-matcher-extra" % "3.6.4",
  cache,
  filters,
  ws,
  "org.mashupbots.socko" %% "socko-webserver" % "0.6.0" // server for tests
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

lazy val addBuildNumber = TaskKey[Unit]("addBuildNumber", "Adds build number parameter to the conf file")

addBuildNumber := {

  val log = streams.value.log

  log.info("Getting build number ...")

  if (sys.env.contains("SEMAPHORE_BUILD_NUMBER")) {
    Files.write(Paths.get("conf/build.conf"), ("buildNumber=" + sys.env("SEMAPHORE_BUILD_NUMBER")).getBytes())
    log.info("Running build #" + sys.env("SEMAPHORE_BUILD_NUMBER"))
  } else {
    log.info("No build number found, local version running ...")
  }
}

(packageZipTarball in Universal) <<= (packageZipTarball in Universal).dependsOn(addBuildNumber)

// Remove package documentation from the generated dist
sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

// Change configuration during tests
javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
