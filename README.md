SBT plugin for creating [RiffRaff](https://github.com/guardian/deploy) deployable artifacts
===========================================================================================

Add
```
addSbtPlugin("com.gu" % "riffraff-artifact" % "0.6.1")
```

to your `project/plugins.sbt` and if you want to bundle your app as a `tgz` using 
[sbt-native-packager](https://github.com/sbt/sbt-native-packager)

```
import com.typesafe.sbt.packager.Keys._

riffRaffPackageType := (packageZipTarball in config("universal")).value

lazy val root = (project in file(".")).enablePlugins(RiffRaffArtifact)
```

to your build.sbt, then run `riffRaffArtifact` to build an artifact deployable by
[RiffRaff](https://github.com/guardian/deploy).

Customisation
-------------

If you follow the above steps the `riffraffArtifact` command will produce an archive called `artifacts.zip` in the 
`target/riffraff` directory. This will contain a `deploy.json` from the `resourceDirectory` (`conf` for a Play app) in 
the root and a packages directory with a sub-directory, named using the project name, containing a `tgz` archive and 
the file with either the extension `.service` (Systemd) or `.conf` (Upstart) and the name of the project found in the projects base 
directory. e.g. for a project with the name `example`

```
deploy.json
packages/example/example.tgz
packages/example/example.service
```

If you want a `zip` rather than a `tgz` built, instead add the following to your `build.sbt`.

```
import com.typesafe.sbt.packager.Keys._
```
for sbt-native-packager 0.8.0, OR
```
enablePlugins(SbtNativePackager)

enablePlugins(JavaAppPackaging)
```
for sbt-native-packager 1.0.0,

followed by

```
riffRaffPackageType := (dist in config("universal")).value

lazy val root = (project in file(".")).enablePlugins(RiffRaffArtifact)
```

If you prefer bundling your app as an uber-jar, instead include the
[sbt-assembly](https://github.com/sbt/sbt-assembly) plugin and add

```
import sbtassembly.Plugin.AssemblyKeys._

riffRaffPackageType := assembly.value

lazy val root = (project in file(".")).enablePlugins(RiffRaffArtifact)
```
