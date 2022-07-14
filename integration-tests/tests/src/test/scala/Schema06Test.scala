import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.{BatchedUpdate, Config, Graph}
import overflowdb.traversal._
import testschema06._
import testschema06.nodes._
import testschema06.edges._
import testschema06.traversal._

class Schema06Test extends AnyWordSpec with Matchers {

  "working with graph, DiffGraph etc." in {
    val node1New = NewNode1().name("node 1")
    val node2aNew = NewNode2().containedAnyNode(node1New)
    val builder = new BatchedUpdate.DiffGraphBuilder
    builder.addNode(node1New)
    builder.addNode(node2aNew)
    // builder.addEdge(node2aNew, node1New, Edge1.Label)
    val graph = TestSchema.empty.graph
    BatchedUpdate.applyDiff(graph, builder)

    // TODO generate node type starters
    def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
    def node2Traversal = graph.nodes(Node2.Label).cast[Node2]

    val node1 = node1Traversal.head
    val node2 = node2Traversal.head
    // ensure contained nodes have the correct types - they should both be StoredNodes
    val innerNode: Option[StoredNode] = node2.containedAnyNode
    innerNode.get shouldBe node1



  }

}
