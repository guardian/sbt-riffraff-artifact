name := "sbt-riffraff-artifact"
organization := "com.gu"

sbtPlugin := true

scalaVersion := "2.12.3"
sbtVersion in Global := "1.0.0"
crossSbtVersions := Seq("1.0.0", "0.13.16")

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.8.1",
  "org.joda" % "joda-convert" % "1.7" % "provided",
  "com.lihaoyi" %% "upickle" % "0.4.4",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.368",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.0.1.201806211838-r",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.2", // bump to remove vulnerable version pulled in by AWS
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalamock" %% "scalamock" % "4.0.0" % "test"
)

fork in Test := false

publishMavenStyle := true

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(ScmInfo(url("https://github.com/guardian/sbt-riffraff-artifact"), "scm:git@github.com:guardian/sbt-riffraff-artifact"))
homepage := scmInfo.value.map(_.browseUrl)
developers := List(Developer(id = "guardian", name = "Guardian", email = null, url = url("https://github.com/guardian")))

// Release
import ReleaseTransformations._
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishTo := sonatypePublishToBundle.value
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
