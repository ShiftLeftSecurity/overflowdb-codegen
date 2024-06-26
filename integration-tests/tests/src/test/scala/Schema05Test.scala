import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.{Config, Graph}
import testschema05._
import testschema05.edges._
import testschema05.nodes._
import testschema05.traversal._
import scala.jdk.CollectionConverters.IteratorHasAsScala

class Schema05Test extends AnyWordSpec with Matchers {
  import overflowdb.traversal._
  "default property values: all empty" in {
    val graph = TestSchema.empty.graph

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

    graph.nodes(Node1.Label).asScala.cast[Node1].str.size shouldBe 0
    graph.edges(Edge1.Label).asScala.property(Edge1.Properties.Str).size shouldBe 0
  }

  "defined property values" in {
    val graph = TestSchema.empty.graph

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]
    val properties = Seq(
      Properties.Bool.of(false),
      Properties.Str.of("foo"),
      Properties.Byte.of(100: Byte),
      Properties.Short.of(101: Short),
      Properties.Int.of(102),
      Properties.Long.of(103),
      Properties.Float.of(104.4f),
      Properties.Double.of(105.5),
      Properties.Char.of('Z'),
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

    def node1Trav = graph.nodes(Node1.Label).asScala.cast[Node1]
    def edge1Trav = graph.edges(Edge1.Label).asScala.cast[Edge1]
    node1Trav.str.next() shouldBe "foo"
    node1Trav.property(Node1.Properties.Str).next() shouldBe "foo"
    edge1Trav.property(Edge1.Properties.Str).next() shouldBe "foo"
  }

  "generated string property filters" in {
    val graph = TestSchema.empty.graph
    val node1 = graph.addNode(Node1.Label, Node1.PropertyNames.Str, "node1 name")
    val node2 = graph.addNode(Node1.Label, Node1.PropertyNames.Str,
      """node2 name line 1
        |node2 name line 2""")
    val node3 = graph.addNode(Node1.Label)
    def node1Traversal = graph.nodes(Node1.Label).asScala.cast[Node1]

    node1Traversal.size shouldBe 3
    node1Traversal.str(".*").size shouldBe 2
    node1Traversal.str(".*name.*").size shouldBe 2
    node1Traversal.str(".*node1.*").size shouldBe 1
    node1Traversal.str(".*line 2.*").size shouldBe 1 // testing multi line matcher
    node1Traversal.str("nomatch", ".*line 2.*").size shouldBe 1
    node1Traversal.strExact("node1 name").size shouldBe 1
    node1Traversal.strExact("nomatch", "node1 name").size shouldBe 1
    node1Traversal.strNot(".*node1.*").size shouldBe 1
    node1Traversal.strNot(".*line 2.*").size shouldBe 1 // testing multi line matcher
    node1Traversal.strNot("nomatch", ".*line 2.*").size shouldBe 1
  }
}
