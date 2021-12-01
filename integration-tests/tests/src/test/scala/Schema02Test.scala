import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.traversal._
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

  "NewNode" can {
    "be used as a product, e.g. for pretty printing" in {
      val newNode = NewNode1().name("A").order(1)

      newNode.productPrefix shouldBe "NewNode1"
      newNode.productArity shouldBe 2
      newNode.productElementName(0) shouldBe "order"
      newNode.productElement(0) shouldBe Some(1)
      newNode.productElementName(1) shouldBe "name"
      newNode.productElement(1) shouldBe "A"
    }

    "get copied and mutated" in {
      val original = NewNode1().name("A").order(1)

      //verify that we can copy
      val copy = original.copy
      original.isInstanceOf[NewNode1] shouldBe true
      copy.isInstanceOf[NewNode1] shouldBe true
      copy.name shouldBe "A"
      copy.order shouldBe Some(1)

      //verify that copy preserved the static type, such that assignment is available
      copy.name = "B"
      copy.order = Some(2)
      copy.name shouldBe "B"
      copy.order shouldBe Some(2)

      copy.name("C")
      copy.name shouldBe "C"
      copy.order(3)
      copy.order shouldBe Some(3)

      copy.order(Some(4: Integer))
      copy.order shouldBe Some(4)

      original.name shouldBe "A"
      original.order shouldBe Some(1)
    }

    "copied and mutated when upcasted" in {
      val original = NewNode1().name("A").order(1)
      val upcasted = original.asInstanceOf[NewNode with HasNameMutable]
      upcasted.name = "A"
      val upcastedCopy = upcasted.copy
      upcastedCopy.name = "C"

      upcastedCopy.name shouldBe "C"
    }

    "copied and mutated when upcasted (2)" in {
      val original = NewNode1().name("A").order(1)
      val upcasted: BaseNodeNew = original

      val upcastedCopy = upcasted.copy
      upcastedCopy.name = "C"
      upcastedCopy.name shouldBe "C"
    }

    "be used as a Product" in {
      val newNode = NewNode1().name("A").order(1)
      newNode.productArity shouldBe 2
      newNode.productElement(0) shouldBe Some(1)
      newNode.productElement(1) shouldBe "A"
      newNode.productPrefix shouldBe "NewNode1"
    }
  }

  "working with a concrete sample graph" can {
    val graph = TestSchema.empty.graph

    val node1 = graph.addNode(Node1.Label, Node1.PropertyNames.Name, "node 01", PropertyNames.ORDER, 4)
    val node2 = graph.addNode(Node2.Label, PropertyNames.NAME, "node 02", PropertyNames.ORDER, 3)
    node1.addEdge(Edge1.Label, node2)
    node2.addEdge(Edge2.Label, node1, PropertyNames.NAME, "edge 02")

    def baseNodeTraversal = graph.nodes(Node1.Label).cast[BaseNode]
    def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
    def node2Traversal = graph.nodes(Node2.Label).cast[Node2]

    "lookup and traverse nodes/edges via domain specific dsl" in {
      val baseNode = baseNodeTraversal.head
      val node1 = node1Traversal.head
      val node2 = node2Traversal.head

      baseNode.label shouldBe Node1.Label
      node1.label shouldBe Node1.Label
      node2.label shouldBe Node2.Label

      baseNode.edge2In.l shouldBe Seq(node2)
      baseNode.edge1Out.l shouldBe Seq(node2)

      // use the custom defined stepNames from schema definition
      val baseNodeToNode2: Traversal[Node2] = baseNode.customStepName1
  // TODO      baseNodeTraversal.customStepName1  - same for others
      baseNodeToNode2.l shouldBe Seq(node2)

      val baseNodeToNode2ViaEdge2: Node2 = baseNode.customStepName2Inverse
      baseNodeToNode2ViaEdge2 shouldBe node2

      val node1ToNode2: Traversal[Node2] = node1.customStepName1
      node1ToNode2.l shouldBe Seq(node2)

      val node1ToNode2ViaEdge2: Node2 = node1.customStepName2Inverse
      node1ToNode2ViaEdge2 shouldBe node2

      // TODO same for the inverse
      node2.customStepNameInverse
//      ???

      // if no specific stepName is defined, we still generate an 'internal' (with `_` prefix) accessor
      val baseNodeToNode2ViaEdge2In: Node2 = baseNode.customStepName2Inverse
      baseNodeToNode2ViaEdge2In shouldBe node2
      //      val node2Trav2: Traversal[Node2] = baseNode.stepName1
      //      node2Trav2.l shouldBe Seq(node2)
      //      val _: Node2 = baseNode._node2ViaEdge2In

    }

    "property filters" in {
      baseNodeTraversal.name.l shouldBe Seq("node 01")
      baseNodeTraversal.name(".*").size shouldBe 1
      baseNodeTraversal.nameExact("node 01").size shouldBe 1
      baseNodeTraversal.nameNot("abc").size shouldBe 1

      node1Traversal.order.l shouldBe Seq(4)
      node1Traversal.orderGt(3).size shouldBe 1
      node1Traversal.orderLt(4).size shouldBe 0
      node1Traversal.orderLte(4).size shouldBe 1
    }
  }
}
