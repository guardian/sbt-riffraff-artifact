package com.gu.riffraff.artifact

import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.sbt.SbtGit.git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.joda.time.{DateTimeZone, DateTime}
import sbt._
import sbt.Keys._
import upickle.default._
import upickle.Js

object RiffRaffArtifact extends AutoPlugin {

  import autoImport._

  override def projectSettings = defaultSettings

  object autoImport {
    lazy val riffRaffArtifact = taskKey[File]("Builds a deployable artifact for RiffRaff")

    lazy val riffRaffPackageType = taskKey[File]("The package to build to then wrap for RiffRaff")
    lazy val riffRaffArtifactResources = taskKey[Seq[(File, String)]]("Files that will be collected by the deployment-artifact task")
    lazy val riffRaffArtifactDirectory = settingKey[String]("Directory within target directory to write the artifact")
    lazy val riffRaffArtifactFile = settingKey[String]("Filename of the artifact built by deployment-artifact")
    lazy val riffRaffPackageName = settingKey[String]("Name of the magenta package")
    lazy val riffRaffArtifactPublishPath = settingKey[String]("Path to tell TeamCity to publish the artifact on")

    lazy val riffRaffManifest = taskKey[File]("Creates a file representing a build for RiffRaff to consume")

    lazy val riffRaffManifestFile = settingKey[String]("Filename of the build manifest for RiffRaff")
    lazy val riffRaffManifestProjectName = settingKey[String]("Project name for the manifest RiffRaff uses to describe a build")
    lazy val riffRaffBuildIdentifier = settingKey[String]("Identifier for a particular build of a project")
    lazy val riffRaffManifestBuildStartTime = taskKey[DateTime]("When the build of this artifact started")
    lazy val riffRaffManifestRevision = taskKey[String]("Revision of the repository the artifact was built from")
    lazy val riffRaffManifestVcsUrl = taskKey[String]("URL of the repository the artifact was built from")
    lazy val riffRaffManifestBranch = taskKey[String]("Branch of the repository the artifact was built from")

    lazy val riffRaffUpload = taskKey[Unit]("Upload artifact and manifest to S3 buckets")
    lazy val riffRaffUploadArtifactBucket = settingKey[Option[String]]("Bucket to upload artifacts to")
    lazy val riffRaffUploadManifestBucket = settingKey[Option[String]]("Bucket to upload manifest to")

    lazy val defaultSettings = Seq(
      riffRaffArtifactFile := "artifacts.zip",
      riffRaffPackageName := name.value,
      riffRaffManifestProjectName := name.value,
      riffRaffArtifactPublishPath := ".",
      riffRaffArtifactDirectory := "riffraff",

      riffRaffManifestFile := "build.json",
      riffRaffManifestBuildStartTime := DateTime.now(),
      riffRaffBuildIdentifier := "unknown",
      riffRaffManifestRevision := git.gitHeadCommit.value.getOrElse("Unknown"),
      riffRaffManifestVcsUrl :=
        new FileRepositoryBuilder().findGitDir(baseDirectory.value).build.getConfig.getString("remote", "origin", "url"),
      riffRaffManifestBranch := git.gitCurrentBranch.value,

      riffRaffUploadArtifactBucket := None,
      riffRaffUploadManifestBucket := None,

      riffRaffArtifactResources := Seq(
        // systemd unit
        baseDirectory.value / s"${riffRaffPackageName.value}.service" ->
          s"packages/${riffRaffPackageName.value}/${riffRaffPackageName.value}.service",

        // upstart script
        baseDirectory.value / s"${riffRaffPackageName.value}.conf" ->
          s"packages/${riffRaffPackageName.value}/${riffRaffPackageName.value}.conf",

        // compressed redistributable
        riffRaffPackageType.value ->
          s"packages/${riffRaffPackageName.value}/${riffRaffPackageType.value.getName}",

        // deploy instructions
        (resourceDirectory in Compile).value / "deploy.json" -> "deploy.json",
        baseDirectory.value / "deploy.json" -> "deploy.json"
      ),

      riffRaffManifest := {
        implicit val dateTimeWriter = Writer[DateTime](dt => Js.Str(dt.withZone(DateTimeZone.UTC).toString))
        val manifestString = write(BuildManifest(
          riffRaffManifestProjectName.value,
          riffRaffBuildIdentifier.value,
          riffRaffManifestBuildStartTime.value,
          riffRaffManifestRevision.value,
          riffRaffManifestVcsUrl.value,
          riffRaffManifestBranch.value
        ))
        val manifestFile = target.value / riffRaffArtifactDirectory.value / riffRaffManifestFile.value
        IO.write(manifestFile, manifestString)
        streams.value.log.info(s"Created RiffRaff manifest: ${manifestFile.getPath}")

        manifestFile
      },

      riffRaffArtifact := {
        val distFile = target.value / riffRaffArtifactDirectory.value / riffRaffArtifactFile.value
        streams.value.log.info(s"Creating RiffRaff artifact $distFile")

        createArchive(riffRaffArtifactResources.value, distFile)

        // Tells TeamCity to publish the artifact => leave this println in here
        // see https://confluence.jetbrains.com/display/TCD9/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-PublishingArtifactswhiletheBuildisStillinProgress
        // for more info.
        println(s"##teamcity[publishArtifacts '$distFile => ${riffRaffArtifactPublishPath.value}']")

        streams.value.log.info("RiffRaff artifact created")
        distFile
      },

      riffRaffUpload := {
        val client = new AmazonS3Client()

        def upload(
          bucketSetting: SettingKey[Option[String]], maybeBucket: Option[String],
          fileTask: TaskKey[File], file: File
        ): Unit = {
          maybeBucket match {
            case Some(bucket) => {
              client.putObject(
                bucket,
                s"${riffRaffPackageName.value}/${riffRaffBuildIdentifier.value}/${file.getName}",
                file
              )
              streams.value.log.info(s"${fileTask.key.label} uploaded")
            }
            case None =>
              streams.value.log.warn(
                s"${bucketSetting.key.label} not specified, cannot upload ${fileTask.key.label}"
              )
          }
        }

        upload(
          riffRaffUploadManifestBucket, riffRaffUploadManifestBucket.value,
          riffRaffManifest, riffRaffManifest.value
        )
        upload(
          riffRaffUploadArtifactBucket, riffRaffUploadArtifactBucket.value,
          riffRaffArtifact, riffRaffArtifact.value
        )
      }
    )
  }

  def createArchive(filesToInclude: Seq[(File, String)], archiveToCreate: File): Unit = {
    if (archiveToCreate.exists()) {
      archiveToCreate.delete()
    }
    IO.zip(filesToInclude, archiveToCreate)
  }

  case class BuildManifest(
    projectName: String,
    buildNumber: String,
    startTime: DateTime,
    revision: String,
    vcsURL: String,
    branch: String
  )
}
