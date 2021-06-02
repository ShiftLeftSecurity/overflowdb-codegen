import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import testschema01._
import testschema01.nodes._

class SchemaTest extends AnyWordSpec with Matchers {

  "generated constants" in {
    PropertyNames.NAME shouldBe "NAME"
    Properties.ORDER.name shouldBe "ORDER"
    PropertyNames.ALL.contains("OPTIONS") shouldBe true
    Properties.ALL.contains(Properties.OPTIONS) shouldBe true

    NodeTypes.NODE1 shouldBe "NODE1"
    NodeTypes.ALL.contains(Node2.Label) shouldBe true

    Node1.Properties.Name.name shouldBe PropertyNames.NAME


  }

}
