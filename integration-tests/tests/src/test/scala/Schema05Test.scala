import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.traversal._
import overflowdb.{Config, Graph}
import testschema05._
import testschema05.edges._
import testschema05.nodes._
import testschema05.traversal._

class Schema05Test extends AnyWordSpec with Matchers {

  "default property values: all empty" in {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]

    node1.bool shouldBe None
    node1.str shouldBe None
    node1.byte shouldBe None
    node1.short shouldBe None
    node1.int shouldBe None
    node1.long shouldBe None
    node1.float shouldBe None
    node1.double shouldBe None
    node1.char shouldBe None
    node1.propertiesMap.isEmpty shouldBe true

    edge1.bool shouldBe None
    edge1.str shouldBe None
    edge1.byte shouldBe None
    edge1.short shouldBe None
    edge1.int shouldBe None
    edge1.long shouldBe None
    edge1.float shouldBe None
    edge1.double shouldBe None
    edge1.char shouldBe None
    edge1.propertiesMap.isEmpty shouldBe true

    graph.nodes(Node1.Label).cast[Node1].str.size shouldBe 0
    graph.edges(Edge1.Label).property(Edge1.Properties.Str).size shouldBe 0
  }

  "defined property values" in {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]
    val properties = Seq(
      Properties.BOOL.of(false),
      Properties.STR.of("foo"),
      Properties.BYTE.of(100: Byte),
      Properties.SHORT.of(101: Short),
      Properties.INT.of(102),
      Properties.LONG.of(103),
      Properties.FLOAT.of(104.4f),
      Properties.DOUBLE.of(105.5),
      Properties.CHAR.of('Z'),
    )
    properties.foreach(node1.setProperty)
    properties.foreach(edge1.setProperty)

    node1.bool shouldBe Some(false)
    node1.str shouldBe Some("foo")
    node1.byte shouldBe Some(100)
    node1.short shouldBe Some(101)
    node1.int shouldBe Some(102)
    node1.long shouldBe Some(103)
    node1.float shouldBe Some(104.4f)
    node1.double shouldBe Some(105.5)
    node1.char shouldBe Some('Z')
    node1.propertyKeys().contains("STR") shouldBe true
    node1.property(Node1.Properties.Str) shouldBe "foo"
    node1.property("DOESNT_EXIST") shouldBe null
    node1.propertiesMap.get("STR") shouldBe "foo"

    edge1.bool shouldBe Some(false)
    edge1.str shouldBe Some("foo")
    edge1.byte shouldBe Some(100)
    edge1.short shouldBe Some(101)
    edge1.int shouldBe Some(102)
    edge1.long shouldBe Some(103)
    edge1.float shouldBe Some(104.4f)
    edge1.double shouldBe Some(105.5)
    edge1.char shouldBe Some('Z')
    edge1.propertyKeys().contains("STR") shouldBe true
    edge1.property(Node1.Properties.Str) shouldBe "foo"
    edge1.property("DOESNT_EXIST") shouldBe null
    edge1.propertiesMap.get("STR") shouldBe "foo"

    def node1Trav = graph.nodes(Node1.Label).cast[Node1]
    def edge1Trav = graph.edges(Edge1.Label).cast[Edge1]
    node1Trav.str.head shouldBe "foo"
    node1Trav.property(Node1.Properties.Str).head shouldBe "foo"
    edge1Trav.property(Edge1.Properties.Str).head shouldBe "foo"
  }
}
