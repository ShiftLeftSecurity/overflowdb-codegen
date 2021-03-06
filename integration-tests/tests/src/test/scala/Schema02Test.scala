import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.traversal._
import overflowdb.{Config, Graph}
import testschema02._
import testschema02.edges._
import testschema02.nodes._
import testschema02.traversal._

class Schema02Test extends AnyWordSpec with Matchers {

  "constants" in {
    BaseNode.Properties.Name.name shouldBe "NAME"
    BaseNode.PropertyNames.Name shouldBe "NAME"
    BaseNode.Edges.Out shouldBe Array(Edge1.Label)
    BaseNode.Edges.In shouldBe Array(Edge2.Label)

    Node1.Properties.Name.name shouldBe "NAME"
    Node1.PropertyNames.Name shouldBe "NAME"
    Node1.Edges.Out shouldBe Array(Edge1.Label)
    Node1.Edges.In shouldBe Array(Edge2.Label)
  }

  "working with a concrete sample graph" can {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label, Node1.PropertyNames.Name, "node 01", PropertyNames.ORDER, 4)
    val node2 = graph.addNode(Node2.Label, PropertyNames.NAME, "node 02", PropertyNames.ORDER, 3)
    node1.addEdge(Edge1.Label, node2)
    node2.addEdge(Edge2.Label, node1, PropertyNames.NAME, "edge 02")

    "lookup and traverse nodes/edges/properties" in {
      def baseNodeTraversal = graph.nodes(Node1.Label).cast[BaseNode]
      val baseNode = baseNodeTraversal.head
      baseNode.edge2In.l shouldBe Seq(node2)
      baseNode.edge1Out.l shouldBe Seq(node2)
      baseNode._node2ViaEdge2In shouldBe node2
      baseNode._node2ViaEdge1Out.l shouldBe Seq(node2)

      baseNodeTraversal.name.l shouldBe Seq("node 01")
      baseNodeTraversal.name(".*").size shouldBe 1
      baseNodeTraversal.nameExact("node 01").size shouldBe 1
      baseNodeTraversal.nameNot("abc").size shouldBe 1

      def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
      node1Traversal.order.l shouldBe Seq(4)
      node1Traversal.orderGt(3).size shouldBe 1
      node1Traversal.orderLt(4).size shouldBe 0
      node1Traversal.orderLte(4).size shouldBe 1
    }
  }
}
