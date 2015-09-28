SBT plugin for creating [RiffRaff](https://github.com/guardian/deploy) deployable artifacts
===========================================================================================

Add
```
addSbtPlugin("com.gu" % "riffraff-artifact" % "0.8.0")
```

to your `project/plugins.sbt` and if you want to bundle your app as a `tgz` using 
[sbt-native-packager](https://github.com/sbt/sbt-native-packager) 

```
enablePlugins(RiffRaffArtifact, UniversalPlugin)

riffRaffPackageType := (packageZipTarball in Universal).value

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := "riffraff-artifact"
riffRaffUploadManifestBucket := "riffraff-builds"
```

to your build.sbt, then run `riffRaffUpload` to build an artifact deployable by
[RiffRaff](https://github.com/guardian/deploy) and upload it to the S3 bukets used
by the Guardian's RiffRaff installation, along with JSON description of the build. 

In order to have the plugin upload to S3, you will also need to have credentials that
can upload to those buckets resolvable by the [DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). Commonly this would be via AWS_ACCESS_KEY_ID and 
AWS_SECRET_ACCESS_KEY environment variables. Travis has [instructions](http://docs.travis-ci.com/user/environment-variables/#Encrypting-Variables-Using-a-Public-Key) 
on how to encrypt these variables.

Customisation
-------------

If you follow the above steps the `riffRaffArtifact` command will produce an archive called `artifacts.zip` in the 
`target/riffraff` directory. This will contain a `deploy.json` from the `resourceDirectory` (`conf` for a Play app) in 
the root and a packages directory with a sub-directory, named using the project name, containing a `tgz` archive and 
the file with either the extension `.service` (Systemd) or `.conf` (Upstart) and the name of the project found in the projects base 
directory. e.g. for a project with the name `example`

```
deploy.json
packages/example/example.tgz
packages/example/example.service
```

If you want a `deb` rather than a `tgz` built, instead add
```
enablePlugins(RiffRaffArtifact, JDebPackaging)

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd
debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "The Maintainer <the.maintainer@company.com>"
packageSummary := "Brief description"
packageDescription := """Slightly longer description"""

riffRaffPackageType := (packageBin in Debian).value

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := "riffraff-artifact"
riffRaffUploadManifestBucket := "riffraff-builds"
```
to your build.sbt. If you prefer bundling your app as an uber-jar, instead include the 
[sbt-assembly](https://github.com/sbt/sbt-assembly) plugin and add

```
enablePlugins(RiffRaffArtifact)

riffRaffPackageType := assembly.value

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := "riffraff-artifact"
riffRaffUploadManifestBucket := "riffraff-builds"
```
