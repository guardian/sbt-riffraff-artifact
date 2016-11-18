package com.gu.riffraff.artifact

import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.joda.time.{DateTime, DateTimeZone}
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

    lazy val riffRaffAwsCredentialsProfile = settingKey[Option[String]]("AWS credentials profile used to upload to S3")
    lazy val riffRaffCredentialsProvider = settingKey[AWSCredentialsProvider]("AWS Credentials provider used to upload to S3")

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

    lazy val riffRaffNotifyTeamcity = taskKey[Unit]("Task to notify teamcity")

    lazy val riffRaffUseYamlConfig = settingKey[Boolean]("True if using the new riff-raff.yaml config file rather than the legacy deploy.json")

    lazy val riffRaffVcsBaseDirectory = settingKey[File]("The base directory that has been checked out from the VCS")

    lazy val riffRaffBuildInfo = settingKey[BuildInfo]("A case class describing VCS specific properties like revision branch etc.")

    lazy val coreSettings = Seq(
      riffRaffPackageName := name.value,
      riffRaffManifestProjectName := name.value,
      riffRaffArtifactPublishPath := ".",
      riffRaffArtifactDirectory := "riffraff",

      riffRaffAwsCredentialsProfile := None,

      riffRaffCredentialsProvider :=
        riffRaffAwsCredentialsProfile.value.foldLeft[AWSCredentialsProvider](new DefaultAWSCredentialsProviderChain()){case (chain, profile) =>
          new AWSCredentialsProviderChain(new ProfileCredentialsProvider(profile), chain)
        },

      riffRaffManifestFile := "build.json",
      riffRaffManifestBuildStartTime := DateTime.now(),
      riffRaffVcsBaseDirectory := baseDirectory.value,
      riffRaffBuildInfo := BuildInfo(riffRaffVcsBaseDirectory.value),
      riffRaffBuildIdentifier := riffRaffBuildInfo.value.buildIdentifier,
      riffRaffManifestRevision := riffRaffBuildInfo.value.revision,
      riffRaffManifestVcsUrl := riffRaffBuildInfo.value.url,
      riffRaffManifestBranch := riffRaffBuildInfo.value.branch,

      riffRaffUploadArtifactBucket := None,
      riffRaffUploadManifestBucket := None,

      riffRaffUseYamlConfig := (baseDirectory.value / "riff-raff.yaml").exists || ((resourceDirectory in Compile).value / "riff-raff.yaml").exists,

      riffRaffArtifactResources := {
        val configFileName = if (riffRaffUseYamlConfig.value) "riff-raff.yaml" else "deploy.json"
        val packagePathPrefix = if (riffRaffUseYamlConfig.value) "" else "packages/"
        Seq(
          // systemd unit
          baseDirectory.value / s"${riffRaffPackageName.value}.service" ->
            s"$packagePathPrefix${riffRaffPackageName.value}/${riffRaffPackageName.value}.service",

          // upstart script
          baseDirectory.value / s"${riffRaffPackageName.value}.conf" ->
            s"$packagePathPrefix${riffRaffPackageName.value}/${riffRaffPackageName.value}.conf",

          // compressed redistributable
          riffRaffPackageType.value ->
            s"$packagePathPrefix${riffRaffPackageName.value}/${riffRaffPackageType.value.getName}",

          // deploy instructions
          (resourceDirectory in Compile).value / configFileName -> configFileName,
          baseDirectory.value / configFileName -> configFileName
        ).filter { case (file, _) => file.exists }
      },

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
      }
    )

    lazy val defaultSettings = coreSettings ++ Seq(
      riffRaffNotifyTeamcity := {
        val teamcityPublishDirectory = target.value / riffRaffArtifactDirectory.value / "teamcity"
        IO.delete(teamcityPublishDirectory)

        riffRaffArtifactResources.value.foreach { case (file, targetName) =>
          val targetFile = teamcityPublishDirectory / targetName
          IO.copyFile(file, targetFile)
        }

        println(s"##teamcity[publishArtifacts '$teamcityPublishDirectory => .']")
      },

      riffRaffUpload := {
        val client = new AmazonS3Client(riffRaffCredentialsProvider.value)

        val prefix = s"${riffRaffManifestProjectName.value}/${riffRaffBuildIdentifier.value}"

        upload(
          riffRaffUploadArtifactBucket, riffRaffUploadArtifactBucket.value,
          riffRaffArtifactResources, prefix, riffRaffArtifactResources.value
        )(client, streams.value)
        upload(
          riffRaffUploadManifestBucket, riffRaffUploadManifestBucket.value,
          riffRaffManifest, prefix, riffRaffManifest.value
        )(client, streams.value)
      }
    )

    lazy val legacySettings = coreSettings ++ Seq(
      riffRaffArtifactFile := "artifacts.zip",

      riffRaffNotifyTeamcity := {
        println(s"##teamcity[publishArtifacts '${riffRaffArtifact.value} => ${riffRaffArtifactPublishPath.value}']")
      },

      riffRaffArtifact := {
        val distFile = target.value / riffRaffArtifactDirectory.value / riffRaffArtifactFile.value
        streams.value.log.info(s"Creating RiffRaff artifact $distFile")

        if (riffRaffUseYamlConfig.value) {
          throw new IllegalArgumentException("It doesn't make sense to use a riff-raff.yaml configuration file in the legacy artifacts.zip file - please migrate from legacySettings to the defaultSettings")
        }

        createArchive(riffRaffArtifactResources.value, distFile)

        streams.value.log.info("RiffRaff artifact created")
        distFile
      },

      riffRaffUpload := {
        val client = new AmazonS3Client(riffRaffCredentialsProvider.value)

        val prefix = s"${riffRaffManifestProjectName.value}/${riffRaffBuildIdentifier.value}"

        upload(
          riffRaffUploadArtifactBucket, riffRaffUploadArtifactBucket.value,
          riffRaffArtifact, prefix, riffRaffArtifact.value
        )(client, streams.value)
        upload(
          riffRaffUploadManifestBucket, riffRaffUploadManifestBucket.value,
          riffRaffManifest, prefix, riffRaffManifest.value
        )(client, streams.value)
      }
    )
  }

  def upload(bucketSetting: SettingKey[Option[String]], maybeBucket: Option[String],
    task: TaskKey[_], bucketPrefix: String, file: File)(client: AmazonS3, streams: Keys.TaskStreams): Unit = {
    val fileSeq = Seq(file -> file.getName)
    upload(bucketSetting, maybeBucket, task, bucketPrefix, fileSeq)(client, streams)
  }

  def upload(
    bucketSetting: SettingKey[Option[String]], maybeBucket: Option[String],
    task: TaskKey[_], bucketPrefix: String, filesToUpload: Seq[(File, String)]
  )(client: AmazonS3, streams: Keys.TaskStreams): Unit = {
    maybeBucket match {
      case Some(bucket) => {
        val uploadRequests = filesToUpload.filter {
          case (file, _) => file.isFile
        }.map{ case (file, name) =>
          new PutObjectRequest(
            bucket,
            s"$bucketPrefix/$name",
            file
          ).withCannedAcl(CannedAccessControlList.BucketOwnerFullControl)
        }

        uploadRequests.foreach(client.putObject)

        val friendlyRequests = uploadRequests.map(r => s"s3://${r.getBucketName}/${r.getKey}")
        streams.log.info(s"${task.key.label} uploaded to ${friendlyRequests.mkString(", ")}")
      }
      case None =>
        streams.log.warn(
          s"${bucketSetting.key.label} not specified, cannot upload ${task.key.label}"
        )
    }
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
