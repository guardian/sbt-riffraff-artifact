package com.gu.riffraff.artifact

import sbt._
import sbt.Keys._

object RiffRaffArtifact extends AutoPlugin {

  override def trigger = allRequirements

  import autoImport._

  override def projectSettings = defaultSettings

  object autoImport {
    lazy val riffRaffArtifact = taskKey[File]("Builds a deployable artifact for RiffRaff")

    lazy val riffRaffPackageType = taskKey[File]("The package to build to then wrap for RiffRaff")
    lazy val riffRaffArtifactResources = taskKey[Seq[(File, String)]]("Files that will be collected by the deployment-artifact task")
    lazy val riffRaffArtifactFile = settingKey[String]("Filename of the artifact built by deployment-artifact")
    lazy val riffRaffPackageName = settingKey[String]("Name of the magenta package")

    lazy val defaultSettings = Seq(
      riffRaffArtifactFile := "artifacts.zip",
      riffRaffPackageName := name.value,

      riffRaffArtifactResources := Seq(
        baseDirectory.value / s"${riffRaffPackageName.value}.conf" ->
          s"packages/${riffRaffPackageName.value}/${riffRaffPackageName.value}.conf",
        riffRaffPackageType.value ->
          s"packages/${riffRaffPackageName.value}/${riffRaffPackageType.value.getName}",
        (resourceDirectory in Compile).value / "deploy.json" ->
          "deploy.json"
      ),

      riffRaffArtifact := {
        val distFile = target.value / riffRaffArtifactFile.value
        streams.value.log.info(s"Creating RiffRaff artifact $distFile")

        createArchive(riffRaffArtifactResources.value, distFile)

        // Tells TeamCity to publish the artifact => leave this println in here
        println(s"##teamcity[publishArtifacts '$distFile']")

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
