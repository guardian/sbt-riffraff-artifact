name := "riffraff-artifact"
organization := "com.gu"

sbtPlugin := true

enablePlugins(GitVersioning)

scalaVersion := "2.10.4"

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.8.1",
  "org.joda" % "joda-convert" % "1.7" % "provided",
  "com.lihaoyi" %% "upickle" % "0.3.4",
  "org.scalamacros" %% s"quasiquotes" % "2.0.0" % "provided",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.5.1"
)

publishMavenStyle := false
bintrayOrganization := Some("guardian")
bintrayRepository := "sbt-plugins"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))