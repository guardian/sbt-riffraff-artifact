SBT plugin for creating [RiffRaff](https://github.com/guardian/deploy) deployable artifacts
===========================================================================================

Quickstart
----------

Add
```
resolvers += Resolver.url("Guardian bintray", url("http://dl.bintray.com/guardian/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.gu" % "riffraff-artifact" % "0.2")
```
to your plugins.sbt, then run `riffRaffArtifact` to build something deployable by
[RiffRaff](https://github.com/guardian/deploy).

Customisation
-------------

By default the `riffraffArtifact` command will produce an archive called `artifacts.zip` in the target directory
containing a `deploy.json` from the `resourceDirectory` (`conf` for a Play app) in the root, a packages directory,
a sub-directory of that with the project name containing a `tgz` archive built using the [sbt-native-packager](https://github.com/sbt/sbt-native-packager)
and file with the extension `.conf` and the name of the project found in the projects base directory.

If you want a `zip` rather than a `tgz` built, the add
```
import com.typesafe.sbt.packager.Keys._

riffRaffPackageType := (dist in config("universal")).value
```
to your build.sbt.
