package overflowdb.schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaTest extends AnyWordSpec with Matchers {

  "NeighborInfo.deriveNeighborNodeType" should {
    val defaultNeighborNodeType = "StoredNode"

    def neighborInfoWith(nodeInfos: Seq[NeighborNodeInfo]): NeighborInfo =
       NeighborInfo(edge = null, nodeInfos, offsetPosition = 0)


    "for no (known) neighbor" in {
      neighborInfoWith(nodeInfos = Seq.empty).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    "for one neighbor" in {
      val nodeInfos = Seq(
        NeighborNodeInfo(accessorName = null, className = "Foo", cardinality = null)
      )
      neighborInfoWith(nodeInfos).deriveNeighborNodeType shouldBe "Foo"
    }
  }

}
