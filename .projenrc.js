const { SbtProject } = require('@guardian/projen-scala-sbt');

const plugins = [
  `addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")`,
  `addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")`,
  `addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")`
];

const project = new SbtProject({
  name: 'sbt-riffraff-artifact',
  sbtVersion: "1.4.9",
  sbtPlugins: plugins.map(_ => ({ line: _ })),
  scalaVersion: "2.12.3",
  libraryDependencies: [
    SbtProject.javaDep("joda-time","joda-time", "2.8.1"),
    SbtProject.javaDep("org.joda", "joda-convert", "1.7", "provided"),
    SbtProject.scalaDep("com.lihaoyi", "upickle","0.4.4"),
    SbtProject.javaDep("com.amazonaws", "aws-java-sdk-s3", "1.11.368"),
    SbtProject.javaDep("org.eclipse.jgit", "org.eclipse.jgit", "5.0.1.201806211838-r"),
    SbtProject.javaDep("com.fasterxml.jackson.core", "jackson-databind", "2.8.11.2"),
    SbtProject.scalaDep("org.scalatest", "scalatest", "3.0.1", "test"),
    SbtProject.scalaDep("org.scalacheck", "scalacheck", "1.13.4", "test"),
    SbtProject.scalaDep("org.scalamock", "scalamock", "4.0.0", "test")
  ]
});

project.buildSbtFile.line(`
organization := "com.gu"

sbtPlugin := true

sbtVersion in Global := "1.0.0"
crossSbtVersions := Seq("1.0.0", "0.13.16")

fork in Test := false

publishMavenStyle := true

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(ScmInfo(url("https://github.com/guardian/sbt-riffraff-artifact"), "scm:git@github.com:guardian/sbt-riffraff-artifact"))
homepage := scmInfo.value.map(_.browseUrl)
developers := List(Developer(id = "guardian", name = "Guardian", email = null, url = url("https://github.com/guardian")))

// Release
import ReleaseTransformations._
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishTo := sonatypePublishToBundle.value
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
`);

project.synth();
