package com.gu.riffraff.artifact

import java.io.{File, FileInputStream, IOException}
import java.util.Properties

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import scala.util.Try

case class BuildInfo(
  buildIdentifier: String,
  branch: String,
  revision: String,
  url: String
) {
  override def toString(): String =
    s"""
       |Build identifier = $buildIdentifier
       |Branch = $branch
       |Revision = $revision
       |Url = $url
     """.stripMargin
}

object BuildInfo {

  val unknown = BuildInfo(
    buildIdentifier = "unknown",
    branch = "unknown",
    revision = "unknown",
    url = "unknown"
  )

  def git(baseDirectory: File): Option[BuildInfo] = {
    def env(propName: String): Option[String] = Option(System.getenv(propName))
    val baseRepo = new FileRepositoryBuilder().findGitDir(baseDirectory)
    baseRepo.setMustExist(true)

    for {
      repo <- Try(baseRepo.build()).toOption
      branch <- Option(repo.getBranch)
      url <- Option(repo.getConfig.getString("remote", "origin", "url"))
      revision = Try(ObjectId.toString(repo.resolve("HEAD"))).toOption.getOrElse("unknown")
    } yield BuildInfo(
      buildIdentifier = env("TRAVIS_BUILD_NUMBER") getOrElse "unknown",
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def teamCity: Option[BuildInfo] = {

    def prop(propName: String, props: Properties = System.getProperties): Option[String] = Option(props.getProperty(propName))
    def loadProps(file: String): Option[Properties] = {
      try {
        val props = new Properties()
        props.load(new FileInputStream(file))
        Some(props)
      } catch {
        case e: IOException =>
          e.printStackTrace()
          None
      }
    }

    for {
      tcPropFile <- prop("teamcity.configuration.properties.file")
      tcProps <- loadProps(tcPropFile)
      buildIdentifier <- prop("build.number", tcProps)
      revision <- prop("build.vcs.number", tcProps)
      branch <- prop("teamcity.build.branch", tcProps)
      url <- prop("vcsroot.url", tcProps)
    } yield BuildInfo(
      buildIdentifier = buildIdentifier,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def apply(baseDirectory: File): BuildInfo = teamCity orElse git(baseDirectory) getOrElse unknown
}
