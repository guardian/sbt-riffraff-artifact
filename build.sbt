import bintray.Keys._
import com.typesafe.sbt.SbtGit._

name := "riffraff-artifact"

organization := "com.gu"

versionWithGit

sbtPlugin := true

scalaVersion := "2.10.4"

publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := Some("guardian")
