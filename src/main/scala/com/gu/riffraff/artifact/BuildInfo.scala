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

  val UNKNOWN = "unknown"
  def env(propName: String): Option[String] = sys.env.get(propName)

  val unknown = BuildInfo(
    buildIdentifier = UNKNOWN,
    branch = UNKNOWN,
    revision = UNKNOWN,
    url = UNKNOWN
  )

  def git(baseDirectory: File): Option[BuildInfo] = {
    val baseRepo = new FileRepositoryBuilder().findGitDir(baseDirectory)
    baseRepo.setMustExist(true)

    for {
      repo <- Try(baseRepo.build()).toOption
      branch <- Option(repo.getBranch)
      url <- Option(repo.getConfig.getString("remote", "origin", "url"))
      revision = Try(ObjectId.toString(repo.resolve("HEAD"))).toOption.getOrElse(UNKNOWN)
    } yield BuildInfo(
      buildIdentifier = UNKNOWN,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def travis(): Option[BuildInfo] = {

    // TRAVIS_BRANCH is master when building PRs
    def getTravisBranch: Option[String] = {
      env("TRAVIS_PULL_REQUEST") match {
        case Some("false") => env("TRAVIS_BRANCH").orElse(Some("unknown-branch"))
        case Some(i) => Some(s"pr/$i")
        case None => Some("unknown-branch")
      }
    }

    val baseDirectory = new File(env("TRAVIS_BUILD_DIR").getOrElse("."))
    val baseRepo = new FileRepositoryBuilder().findGitDir(baseDirectory)
    baseRepo.setMustExist(true)
    for {
      repo <- Try(baseRepo.build()).toOption
      buildIdentifier <- env("TRAVIS_BUILD_NUMBER")
      branch <- getTravisBranch
      url <- Option(repo.getConfig.getString("remote", "origin", "url"))
      revision <- env("TRAVIS_COMMIT")
    } yield BuildInfo(
      buildIdentifier = buildIdentifier,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def circleCi(): Option[BuildInfo] = {
    for {
      buildIdentifier <- env("CIRCLE_BUILD_NUM")
      branch <- env("CIRCLE_BRANCH")
      url <- env("CIRCLE_REPOSITORY_URL")
      revision <- env("CIRCLE_SHA1")
    } yield BuildInfo(
      buildIdentifier = buildIdentifier,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def teamCity: Option[BuildInfo] = {

    def prop(propName: String, props: Properties = System.getProperties): Option[String] = {
      Option(props.getProperty(propName))
        .map(_.trim)
        .filter(_.nonEmpty)
    }

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

    def vcsRootBranch(tcProps: Properties): Option[String] = {
      prop("vcsroot.branch", tcProps).map(ref => ref.split("/").lastOption.getOrElse(ref))
    }

    for {
      tcBuildPropsFile <- env("TEAMCITY_BUILD_PROPERTIES_FILE")
      tcBuildProps <- loadProps(tcBuildPropsFile)
      tcConfigPropFile <- prop("teamcity.configuration.properties.file", tcBuildProps)
      tcConfigProps <- loadProps(tcConfigPropFile)
      buildIdentifier <- prop("build.number", tcConfigProps)
      revision <- prop("build.vcs.number", tcConfigProps)
      branch <- prop("teamcity.build.branch", tcConfigProps) orElse vcsRootBranch(tcConfigProps)
      url <- prop("vcsroot.url", tcConfigProps)
    } yield BuildInfo(
      buildIdentifier = buildIdentifier,
      branch = branch,
      revision = revision,
      url = url
    )
  }

  def apply(baseDirectory: File): BuildInfo =
    teamCity orElse circleCi orElse travis orElse git(baseDirectory) getOrElse unknown
}
