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
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]

    node1.str shouldBe "<[empty]>"
    node1.valueMap shouldBe Map(
      "STR" -> "<[empty]>",
      "TODO" -> "foo"
    )

    // TODO repeat for node, edge, nodeTrav, edgeTrav
    // TODO for all other property value types
    // TODO for custom defined properties


//      def baseNodeTraversal = graph.nodes(Node1.Label).cast[BaseNode]
//      val baseNode = baseNodeTraversal.head
//      baseNode.edge2In.l shouldBe Seq(node2)
//      baseNode.edge1Out.l shouldBe Seq(node2)
//      baseNode._node2ViaEdge2In shouldBe node2
//      baseNode._node2ViaEdge1Out.l shouldBe Seq(node2)
//
//      baseNodeTraversal.name.l shouldBe Seq("node 01")
//      baseNodeTraversal.name(".*").size shouldBe 1
//      baseNodeTraversal.nameExact("node 01").size shouldBe 1
//      baseNodeTraversal.nameNot("abc").size shouldBe 1
//
//      def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
//      node1Traversal.order.l shouldBe Seq(4)
//      node1Traversal.orderGt(3).size shouldBe 1
//      node1Traversal.orderLt(4).size shouldBe 0
//      node1Traversal.orderLte(4).size shouldBe 1
//    }
  }
}
