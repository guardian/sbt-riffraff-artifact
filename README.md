SBT plugin for creating [RiffRaff](https://github.com/guardian/deploy) deployable artifacts
===========================================================================================

Quickstart
----------

Add
```
addSbtPlugin("com.gu" % "riffraff-artifact" % "0.2")
```
to your plugins.sbt, then run `riffRaffArtifact` to build an artifact deployable by
[RiffRaff](https://github.com/guardian/deploy).

Customisation
-------------

By default the `riffraffArtifact` command will produce an archive called `artifacts.zip` in the target directory. This
will contain a `deploy.json` from the `resourceDirectory` (`conf` for a Play app) in the root and a packages directory
with a sub-directory, named using the project name, containing a `tgz` archive built using the 
[sbt-native-packager](https://github.com/sbt/sbt-native-packager) plugin and the file with the extension `.conf` and 
the name of the project found in the projects base directory. e.g. for a project with the name `example`

```
deploy.json
packages/example/example.tgz
packages/example/example.conf
```

If you want a `zip` rather than a `tgz` built, add
```
import com.typesafe.sbt.packager.Keys._

riffRaffPackageType := (dist in config("universal")).value
```
to your build.sbt.
