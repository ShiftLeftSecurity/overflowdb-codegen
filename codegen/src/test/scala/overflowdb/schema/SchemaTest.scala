package overflowdb.schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaTest extends AnyWordSpec with Matchers {

  "NeighborInfo.deriveNeighborNodeType" when {
    val defaultNeighborNodeType = "StoredNode"
    val baseNodeA = new NodeBaseType(name = "BASE_A", comment = None, SchemaInfo.Unknown)
    val nodeA1 = new NodeType(name = "A1", comment = None, SchemaInfo.Unknown).extendz(baseNodeA)
    val nodeA2 = new NodeType(name = "A2", comment = None, SchemaInfo.Unknown).extendz(baseNodeA)
    val baseNodeB = new NodeBaseType(name = "BASE_B", comment = None, SchemaInfo.Unknown)
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

    "having multiple neighbors with same base type" in {
      neighborInfoWith(Seq(nodeA1, nodeA2)).deriveNeighborNodeType shouldBe baseNodeA.className
      neighborInfoWith(Seq(nodeA1, baseNodeA)).deriveNeighborNodeType shouldBe baseNodeA.className

      // more complex hierarchy: multiple levels
      val baseNodeAExt = new NodeBaseType(name = "BASE_A_EXT", comment = None, SchemaInfo.Unknown).extendz(baseNodeA)
      val nodeAExt1 = new NodeType(name = "A_EXT_1", comment = None, SchemaInfo.Unknown).extendz(baseNodeAExt)
      neighborInfoWith(Seq(nodeA1, nodeAExt1, baseNodeAExt, baseNodeA)).deriveNeighborNodeType shouldBe baseNodeA.className
    }

    // TODO drop debug code
    "foo" in {
      val baseNodeA = new NodeBaseType(name = "BASE_A", comment = None, SchemaInfo.Unknown)
      val nodeA1 = new NodeType(name = "A1", comment = None, SchemaInfo.Unknown).extendz(baseNodeA)

//      val nodeInfos = neighborInfoWith(Seq(nodeA1, baseNodeA)).nodeInfos.map(_.neighborNode).toSet
//      val lcaMaybe = LowestCommonAncestors1(nodeInfos)(_.extendzRecursively.toSet).headOption
//      println(lcaMaybe)
//      x.foreach(println)

//      println(findSharedRoot(nodeInfos))
      println(neighborInfoWith(Seq(nodeA1, baseNodeA)).deriveNeighborNodeType)

    }


    def neighborInfoWith(nodes: Seq[AbstractNodeType]): NeighborInfoForEdge =
      NeighborInfoForEdge(
        edge = null,
        nodes.map(node => NeighborInfoForNode(neighborNode = node, edge = null, direction = null, cardinality = null, isInherited = false)),
        offsetPosition = 0)
  }

}
