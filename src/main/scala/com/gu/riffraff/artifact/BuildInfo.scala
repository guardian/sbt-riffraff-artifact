package com.gu.riffraff.artifact

import java.io.File

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
    val baseRepo = new FileRepositoryBuilder().findGitDir(baseDirectory)
    baseRepo.setMustExist(true)

    for {
      repo <- Try(baseRepo.build()).toOption
      branch <- Option(repo.getBranch)
      url <- Option(repo.getConfig.getString("remote", "origin", "url"))
      revision = Try(ObjectId.toString(repo.resolve("HEAD"))).toOption.getOrElse("unknown")
    } yield BuildInfo(
      buildIdentifier = "unknown",
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def teamCity: Option[BuildInfo] = {
    def prop(propName: String): Option[String] = Option(System.getProperty(propName))
    for {
      buildIdentifier <- prop("build.number")
      revision <- prop("build.vcs.number")
      branch <- prop("vcsroot.branch")
      url <- prop("vcsroot.url")
    } yield BuildInfo(
      buildIdentifier = buildIdentifier,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def apply(baseDirectory: File): BuildInfo = git(baseDirectory) orElse teamCity getOrElse unknown
}
