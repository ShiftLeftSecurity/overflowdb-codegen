import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import testschema01._
import testschema01.nodes._
import testschema01.edges._

class SchemaTest extends AnyWordSpec with Matchers {

  "constants" in {
    PropertyNames.NAME shouldBe "NAME"
    Properties.ORDER.name shouldBe "ORDER"
    PropertyNames.ALL.contains("OPTIONS") shouldBe true
    Properties.ALL.contains(Properties.OPTIONS) shouldBe true

    NodeTypes.NODE1 shouldBe "NODE1"
    NodeTypes.ALL.contains(Node2.Label) shouldBe true

    Node1.Properties.Name.name shouldBe PropertyNames.NAME
    Node1.PropertyNames.Order shouldBe PropertyNames.ORDER
    Node1.Edges.Out shouldBe Array(Edge1.Label)
    Node1.Edges.In shouldBe Array(Edge2.Label)

    Edge2.Properties.Name.name shouldBe PropertyNames.NAME
  }

}
