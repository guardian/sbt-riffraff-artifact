package com.gu.riffraff.artifact

import sbt._
import sbt.Keys._

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

    lazy val defaultSettings = Seq(
      riffRaffArtifactFile := "artifacts.zip",
      riffRaffPackageName := name.value,
      riffRaffArtifactPublishPath := ".",
      riffRaffArtifactDirectory := "riffraff",

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
         // "traditional" location
         (resourceDirectory in Compile).value / "deploy.json" -> "deploy.json",
         // a more logical location
         baseDirectory.value / "deploy.json" -> "deploy.json"

      ),

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
      }
    )
  }

  def createArchive(filesToInclude: Seq[(File, String)], archiveToCreate: File): Unit = {
    if (archiveToCreate.exists()) {
      archiveToCreate.delete()
    }
    IO.zip(filesToInclude, archiveToCreate)
  }
}
