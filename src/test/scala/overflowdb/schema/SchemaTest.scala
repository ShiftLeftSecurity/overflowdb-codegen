package overflowdb.schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaTest extends AnyWordSpec with Matchers {

  "NeighborInfo.deriveNeighborNodeType" when {
    val defaultNeighborNodeType = "StoredNode"
    val baseNodeA = new NodeBaseType(name = "BASE_A", comment = None, SchemaInfo.Unknown)
    val baseNodeB = new NodeBaseType(name = "BASE_B", comment = None, SchemaInfo.Unknown)
    val nodeA1 = new NodeType(name = "A1", comment = None, SchemaInfo.Unknown).extendz(baseNodeA)
    val nodeB2 = new NodeType(name = "B1", comment = None, SchemaInfo.Unknown).extendz(baseNodeB)

    "having no (known) neighbor" in {
      neighborInfoWith(Seq.empty).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    "having one neighbor" in {
      neighborInfoWith(Seq(nodeA1)).deriveNeighborNodeType shouldBe nodeA1.className
      neighborInfoWith(Seq(baseNodeA)).deriveNeighborNodeType shouldBe baseNodeA.className
    }

    "having multiple neighbors with same type (e.g. different edges to same node type)" in {
      neighborInfoWith(Seq(nodeA1, nodeA1)).deriveNeighborNodeType shouldBe nodeA1.className
      neighborInfoWith(Seq(baseNodeA, baseNodeA)).deriveNeighborNodeType shouldBe baseNodeA.className
    }

    "having different, unrelated neighbors" in {
      neighborInfoWith(Seq(nodeA1, nodeB2)).deriveNeighborNodeType shouldBe defaultNeighborNodeType
      neighborInfoWith(Seq(baseNodeA, baseNodeB)).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    def neighborInfoWith(nodes: Seq[AbstractNodeType]): NeighborInfo =
      NeighborInfo(
        edge = null,
        nodes.map(node => NeighborNodeInfo(accessorName = null, node, cardinality = null)),
        offsetPosition = 0)
  }

}
