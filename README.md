SBT plugin for creating [RiffRaff](https://github.com/guardian/deploy) deployable artifacts 
===========================================================================================

[![Build Status](https://travis-ci.org/guardian/sbt-riffraff-artifact.svg?branch=master)](https://travis-ci.org/guardian/sbt-riffraff-artifact)
[ ![Download](https://api.bintray.com/packages/guardian/sbt-plugins/sbt-riffraff-artifact/images/download.svg) ](https://bintray.com/guardian/sbt-plugins/sbt-riffraff-artifact/_latestVersion)

Installation
------------

Add
```scala
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.5")
```

to your `project/plugins.sbt`

Usage
-----

If you want to bundle your app as a `tgz` using 
[sbt-native-packager](https://github.com/sbt/sbt-native-packager) 

```scala
enablePlugins(RiffRaffArtifact, UniversalPlugin)

riffRaffPackageType := (packageZipTarball in Universal).value

def env(key: String): Option[String] = Option(System.getenv(key))
riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
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
```scala
enablePlugins(RiffRaffArtifact, JDebPackaging)

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd
debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "The Maintainer <the.maintainer@company.com>"
packageSummary := "Brief description"
packageDescription := """Slightly longer description"""

riffRaffPackageType := (packageBin in Debian).value

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
```
to your build.sbt. If you prefer bundling your app as an uber-jar, instead include the 
[sbt-assembly](https://github.com/sbt/sbt-assembly) plugin and add

```scala
enablePlugins(RiffRaffArtifact)

riffRaffPackageType := assembly.value

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
```

AWS Configuration
-----------------

In order to upload artifacts and manifest files to the Guardian's RiffRaff buckets, you will need to give your build the appropriate permissions.

1. Make sure your AWS account is in the [list of accounts](https://github.com/guardian/deploy-tools-platform/blob/master/cloudformation/riffraff-buckets.template.yaml) with permission to grant access to those buckets. If it is not, make a PR or ask @philwills to add it for you.

2. Create an IAM user in your AWS account for your build. Give it a policy that looks something like this:

    ```
    {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmt1428934825002",
            "Effect": "Allow",
            "Action": [
                "s3:Put*",
                "s3:List*"
            ],
            "Resource": [
                "arn:aws:s3::*:riffraff-artifact",
                "arn:aws:s3::*:riffraff-artifact/*",
                "arn:aws:s3::*:riffraff-builds",
                "arn:aws:s3::*:riffraff-builds/*"
            ]
        }
    ]
}
    ```

3. Pass the AWS access key and secret key to your build, e.g. as environment variables.

Releasing
---------

This project uses [`sbt-release`](https://github.com/sbt/sbt-release) and [`bintray-sbt`](https://github.com/softprops/bintray-sbt)
to release to [Bintray](https://bintray.com/guardian/sbt-plugins/sbt-riffraff-artifact). You'll need a Bintray account that's been added to the
[`guardian` organisation](https://bintray.com/guardian/) and [your Bintray API key](https://bintray.com/profile/edit).
We also PGP sign our artifacts, so you'll need a PGP key set up for [`sbt-pgp`](http://www.scala-sbt.org/sbt-pgp/) to read.

Once you've got all that, releasing is just:

```
$ sbt release
```

