package com.gu.riffraff.artifact



import org.joda.time.{DateTime, DateTimeZone}
import upickle.Js
import upickle.default._
import upickle.default.write


case class BuildManifest(
  projectName: String,
  buildInfo: BuildInfo,
  startTime: DateTime = DateTime.now()
) {

  def writeManifest :String = {

    implicit val thing2Writer = upickle.default.Writer[BuildManifest]{
      case t => Js.Obj(
        ("projectName", Js.Str(t.projectName)),
        ("startTime", Js.Str(t.startTime.withZone(DateTimeZone.UTC).toString)),
        ("buildNumber", Js.Str(t.buildInfo.buildIdentifier)),
        ("revision", Js.Str(t.buildInfo.revision)),
        ("vcsURL", Js.Str(t.buildInfo.url)),
        ("branch", Js.Str(t.buildInfo.branch))
      )
    }
    write(this)
  }

}
