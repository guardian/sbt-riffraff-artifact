import com.gu.riffraff.artifact.{BuildInfo, BuildManifest}
import org.scalatest.Matchers._
import org.scalatest.FunSuite

class BuildManifestTest extends FunSuite {
  test("Build manifest content looks right") {
    val bi = BuildInfo("myidentifier", "mybranch", "myrevision", "myurl")
    val mc = BuildManifest("myprojectname", bi)
    val content = mc.writeManifest
    content should startWith ("""{"projectName":"myprojectname","startTime":"""")
    content should endWith ("""","buildNumber":"myidentifier","revision":"myrevision","vcsURL":"myurl","branch":"mybranch"}""")
  }

}
