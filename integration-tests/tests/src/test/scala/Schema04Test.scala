import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.traversal._
import overflowdb.{Config, Graph}
import testschema04._
import testschema04.edges._
import testschema04.nodes._
import testschema04.traversal._

class Schema04Test extends AnyWordSpec with Matchers {

  "default property values" in {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
//    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
//    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]

    node1.bool shouldBe true
    node1.str shouldBe "<[empty]>"
    node1.byte shouldBe 1
    node1.short shouldBe 2
    node1.int shouldBe 3
    node1.long shouldBe 4
//    node1.float.isNaN shouldBe true
//    node1.double.isNaN shouldBe true
//    node1.char shouldBe '?'
//    node1.property(Node1.Properties.Str) shouldBe "<[empty]>"
//    node1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    // TODO test all other default properties
//
//    ???
//    edge1.bool shouldBe true
//    edge1.str shouldBe "<[empty]>"
//    edge1.byte shouldBe 0
//    edge1.short shouldBe 0
//    edge1.int shouldBe 0
//    edge1.long shouldBe 0
//    edge1.float.isNaN shouldBe true
//    edge1.double.isNaN shouldBe true
//    edge1.char shouldBe '?'
//    edge1.property(Edge1.Properties.Str) shouldBe "<[empty]>"
//    edge1.propertyDefaultValue("STR") shouldBe "<[empty]>"
//
//    node1.propertiesMap.get("STR") shouldBe "<[empty]>"
//    node1.propertiesMap.get("STR") shouldBe "<[empty]>"
//    edge1.propertiesMap.get("STR") shouldBe "<[empty]>"
//    node1.get.propertiesMapWithoutDefaults.isEmpty shouldBe true
//    graph.nodes(Node1.Label).cast[Node1].str.head shouldBe "<[empty]>"
//    graph.nodes(Node1.Label).cast[Node1].property(Node1.Properties.Str).head shouldBe "<[empty]>"
//    graph.edges(Edge1.Label).cast[Edge1].property(Edge1.Properties.Str).head shouldBe "<[empty]>"
  }
}
