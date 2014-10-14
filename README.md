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
