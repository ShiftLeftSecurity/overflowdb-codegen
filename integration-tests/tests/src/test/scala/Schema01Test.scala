import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.{Config, Graph}
import overflowdb.traversal._
import testschema01._
import testschema01.nodes._
import testschema01.edges._

class Schema01Test extends AnyWordSpec with Matchers {

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

  "working with a concrete sample graph" can {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label, Node1.PropertyNames.Name, "node 01")
    val node2 = graph.addNode(Node2.Label, PropertyNames.NAME, "node 02", PropertyNames.ORDER, 3)
    node1.addEdge(Edge1.Label, node2)
    node2.addEdge(Edge2.Label, node1, PropertyNames.NAME, "edge 02")

    "lookup and traverse nodes/edges/properties" in {
      // generic traversal
      graph.nodes.property(Properties.NAME).toSet shouldBe Set("node 01", "node 02")
      graph.edges.property(Properties.NAME).toSet shouldBe Set("edge 02")
      def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
      node1Traversal.out.toList shouldBe Seq(node2)

      // domain specific traversals (generated by codegen)
      val node1Specific = node1Traversal.head
      node1Specific.edge1Out.l shouldBe Seq(node2)
      node1Specific._node2ViaEdge1Out.l shouldBe Seq(node2)
      node1Specific._node2ViaEdge2In shouldBe node2
    }

    "set properties" in {
      node1.setProperty(Node1.Properties.Name.of("updated"))
      node1.setProperty(Node1.Properties.Order.of(4))

      // TODO generate domain-specific setters in codegen
    }
  }


}
