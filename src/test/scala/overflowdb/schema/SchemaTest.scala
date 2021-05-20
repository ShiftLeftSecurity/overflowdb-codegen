package overflowdb.schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaTest extends AnyWordSpec with Matchers {

  "NeighborInfo.deriveNeighborNodeType" should {
    val defaultNeighborNodeType = "StoredNode"

    "for no (known) neighbor" in {
      neighborInfoWith(Seq.empty).deriveNeighborNodeType shouldBe defaultNeighborNodeType
    }

    "for one neighbor" in {
      neighborInfoWith(Seq("Foo")).deriveNeighborNodeType shouldBe "Foo"
    }

    def neighborInfoWith(nodeClassnames: Seq[String]): NeighborInfo =
      NeighborInfo(
        edge = null,
        nodeClassnames.map(c => NeighborNodeInfo(accessorName = null, className = c, cardinality = null)),
        offsetPosition = 0)
  }

}
