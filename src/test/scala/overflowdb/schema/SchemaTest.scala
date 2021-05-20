package overflowdb.schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaTest extends AnyWordSpec with Matchers {

  "NeighborInfo.deriveNeighborNodeType" when {
    val defaultNeighborNodeType = "StoredNode"
    val nodeA1 = new NodeType(name = "A1", comment = None, SchemaInfo.Unknown)
    val nodeB2 = new NodeType(name = "B1", comment = None, SchemaInfo.Unknown)

    "having no (known) neighbor" in {
      neighborInfoWith(Seq.empty).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    "having one neighbor" in {
      neighborInfoWith(Seq(nodeA1)).deriveNeighborNodeType shouldBe nodeA1.className
    }

    "having multiple neighbors with same type (e.g. different edges to same node type)" in {
      neighborInfoWith(Seq(nodeA1, nodeA1)).deriveNeighborNodeType shouldBe nodeA1.className
    }

    "having different, unrelated neighbors" in {
      neighborInfoWith(Seq(nodeA1, nodeB2)).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    def neighborInfoWith(nodes: Seq[AbstractNodeType]): NeighborInfo =
      NeighborInfo(
        edge = null,
        nodes.map(node => NeighborNodeInfo(accessorName = null, node, cardinality = null)),
        offsetPosition = 0)
  }

}
