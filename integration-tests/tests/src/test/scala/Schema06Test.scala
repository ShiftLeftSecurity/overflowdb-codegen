import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.{BatchedUpdate, Config, Graph}
import testschema06._
import testschema06.nodes._
import testschema06.edges._
import testschema06.traversal._
import scala.jdk.CollectionConverters.IteratorHasAsScala

class Schema06Test extends AnyWordSpec with Matchers {
  import overflowdb.traversal._

  "working with graph, DiffGraph etc." in {
    val node1New = NewNode1().name("node 1")
    val node2New = NewNode2().containedAnyNode(node1New)
    val builder = new BatchedUpdate.DiffGraphBuilder
    builder.addNode(node1New)
    builder.addNode(node2New)
    builder.addEdge(node1New, node2New, Edge1.Label)
    builder.addEdge(node2New, node1New, Edge2.Label)
    val graph = TestSchema.empty.graph
    BatchedUpdate.applyDiff(graph, builder)
   // import overflowdb.traversal.ImplicitsTmp._

    // TODO generate node type starters
    def node1Traversal = graph.nodes(Node1.Label).asScala.cast[Node1]
    def node2Traversal = graph.nodes(Node2.Label).asScala.cast[Node2]

    val node1 = node1Traversal.next()
    val node2 = node2Traversal.next()
    // ensure contained nodes have the correct types - they should both be StoredNodes
    val innerNode: Option[StoredNode] = node2.containedAnyNode
    innerNode.get shouldBe node1

    // verify traversals: node1 <-> node2
    val node2ViaEdge1Out: StoredNode = node1Traversal.edge1OutNamed.next()
    node2ViaEdge1Out shouldBe node2

    val node2ViaEdge2In: StoredNode = node1Traversal.edge2InNamed.next()
    node2ViaEdge2In shouldBe node2
  }

}
