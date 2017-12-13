import scala.collection.immutable.List
import org.scalatest.Matchers._
import com.gu.riffraff.artifact.RiffRaffArtifact
import org.scalatest.FunSuite

import org.scalamock.scalatest.MockFactory

class RiffRaffArtifactTest extends FunSuite with MockFactory {

  test("RiffRaffArtifact should have project configuration") {
    RiffRaffArtifact.projectConfigurations shouldBe a [List[_]]
  }

  test("RiffRaffArtifact should have project settings") {
    RiffRaffArtifact.projectSettings shouldBe a [List[_]]
  }

  test("RiffRaffArtifact should contain project settings") {
    RiffRaffArtifact.projectSettings.size should not be (0)
  }

}
