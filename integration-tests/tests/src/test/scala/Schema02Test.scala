import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.SchemaViolationException
import overflowdb.traversal.Traversal
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
      def baseNode = baseNodeTraversal.head
      def node1 = node1Traversal.head
      def node2 = node2Traversal.head

      baseNode.label shouldBe Node1.Label
      node1.label shouldBe Node1.Label
      node2.label shouldBe Node2.Label

      baseNode.edge2In.l shouldBe Seq(node2)
      baseNode.edge1Out.l shouldBe Seq(node2)
    }

    "generate custom defined stepNames from schema definition" in {
      def baseNode = baseNodeTraversal.head
      def node1 = node1Traversal.head
      def node2 = node2Traversal.head

      val baseNodeToNode2: Traversal[Node2] = baseNode.customStepName1
      baseNodeToNode2.l shouldBe Seq(node2)
      val baseNodeTraversalToNode2: Traversal[Node2] = baseNodeTraversal.customStepName1
      baseNodeTraversalToNode2.l shouldBe Seq(node2)

      val baseNodeToNode2ViaEdge2: Node2 = baseNode.customStepName2Inverse
      baseNodeToNode2ViaEdge2 shouldBe node2
      val baseNodeTraversalToNode2ViaEdge2: Traversal[Node2] = baseNodeTraversal.customStepName2Inverse
      baseNodeTraversalToNode2ViaEdge2.l shouldBe Seq(node2)

      val node1ToNode2: Traversal[Node2] = node1.customStepName1
      node1ToNode2.l shouldBe Seq(node2)
      val node1TraversalToNode2: Traversal[Node2] = node1Traversal.customStepName1
      node1TraversalToNode2.l shouldBe Seq(node2)

      val node1ToNode2ViaEdge2: Node2 = node1.customStepName2Inverse
      node1ToNode2ViaEdge2 shouldBe node2
      val node1TraversalToNode2ViaEdge2: Traversal[Node2] = node1Traversal.customStepName2Inverse
      node1TraversalToNode2ViaEdge2.l shouldBe Seq(node2)

      val node2ToBaseNodeViaEdge2: BaseNode = node2.customStepName2
      node2ToBaseNodeViaEdge2 shouldBe node1
      val node2TraversalToBaseNodeViaEdge2: Traversal[BaseNode] = node2Traversal.customStepName2
      node2TraversalToBaseNodeViaEdge2.l shouldBe Seq(node1)

      val node2ToBaseNodeViaEdge1: Option[BaseNode] = node2.customStepName1Inverse
      node2ToBaseNodeViaEdge1 shouldBe Some(node1)
      val node2TraversalToBaseNodeViaEdge1: Traversal[BaseNode] = node2Traversal.customStepName1Inverse
      node2TraversalToBaseNodeViaEdge1.l shouldBe Seq(node1)
    }

    "property filters" in {
      baseNodeTraversal.name.l shouldBe Seq("node 01")
      baseNodeTraversal.name(".*").size shouldBe 1
      baseNodeTraversal.nameExact("node 01").size shouldBe 1
      baseNodeTraversal.nameExact("not found", "node 01").size shouldBe 1
      baseNodeTraversal.nameNot("abc").size shouldBe 1

      node1Traversal.order.l shouldBe Seq(4)
      node1Traversal.orderGt(3).size shouldBe 1
      node1Traversal.orderLt(4).size shouldBe 0
      node1Traversal.orderLte(4).size shouldBe 1
    }
  }

  "provide detailed error message for schema violation" in {
    val graph = TestSchema.empty.graph
    def baseNodeTraversal = graph.nodes(Node1.Label).cast[BaseNode]
    def node1Traversal = graph.nodes(Node1.Label).cast[Node1]
    def node2Traversal = graph.nodes(Node2.Label).cast[Node2]

    val node1 = graph.addNode(Node1.Label)
    val node2 = graph.addNode(Node2.Label)
    // no edge between these two nodes, which is a schema violation

    the[SchemaViolationException].thrownBy {
      node2Traversal.customStepName2.next()
    }.getMessage should include ("OUT edge with label EDGE2 to an adjacent BASE_NODE is mandatory")

    the[SchemaViolationException].thrownBy {
      node1Traversal.customStepName2Inverse.next()
    }.getMessage should include ("IN edge with label EDGE2 to an adjacent NODE2 is mandatory")

    the[SchemaViolationException].thrownBy {
      baseNodeTraversal.customStepName2Inverse.next()
    }.getMessage should include ("IN edge with label EDGE2 to an adjacent NODE2 is mandatory")
  }

  "marker traits" in {
    classOf[MarkerTrait1].isAssignableFrom(classOf[BaseNode]) shouldBe true
    classOf[MarkerTrait1].isAssignableFrom(classOf[Node1]) shouldBe true
    classOf[MarkerTrait2].isAssignableFrom(classOf[Node2]) shouldBe true

    classOf[MarkerTrait1].isAssignableFrom(classOf[Node2]) shouldBe false
    classOf[MarkerTrait2].isAssignableFrom(classOf[BaseNode]) shouldBe false
    classOf[MarkerTrait2].isAssignableFrom(classOf[Node1]) shouldBe false
  }
}
