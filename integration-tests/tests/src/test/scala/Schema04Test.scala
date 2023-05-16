import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.Graph
import testschema04._
import testschema04.edges._
import testschema04.nodes._
import testschema04.traversal._

import scala.jdk.CollectionConverters.IteratorHasAsScala

import java.nio.file.Files
import scala.collection.immutable.ArraySeq

class Schema04Test extends AnyWordSpec with Matchers {
  import overflowdb.traversal._


  "default property values" in {
    val graph = TestSchema.empty.graph

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]

    node1.bool shouldBe true
    node1.str shouldBe "<[empty]>"
    node1.byte shouldBe 1
    node1.short shouldBe 2
    node1.int shouldBe 3
    node1.long shouldBe 4
    node1.float1 shouldBe 5.5f
    node1.float2.isNaN shouldBe true
    node1.double1 shouldBe 6.6
    node1.double2.isNaN shouldBe true
    node1.char shouldBe '?'
    node1.intList.size shouldBe 0
    node1.propertyKeys().contains("STR") shouldBe true
    node1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    node1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    node1.property(Node1.Properties.Str) shouldBe "<[empty]>"
    node1.property("DOESNT_EXIST") shouldBe null
    node1.propertiesMap.get("STR") shouldBe "<[empty]>"
    node1.get.propertiesMapForStorage.isEmpty shouldBe true

    edge1.bool shouldBe true
    edge1.str shouldBe "<[empty]>"
    edge1.byte shouldBe 1
    edge1.short shouldBe 2
    edge1.int shouldBe 3
    edge1.long shouldBe 4
    edge1.float1 shouldBe 5.5f
    edge1.float2.isNaN shouldBe true
    edge1.double1 shouldBe 6.6
    edge1.double2.isNaN shouldBe true
    edge1.char shouldBe '?'
    edge1.intList.size shouldBe 0
    edge1.propertyKeys().contains("STR") shouldBe true
    edge1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    edge1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    edge1.property(Edge1.Properties.Str) shouldBe "<[empty]>"
    edge1.property("DOESNT_EXIST") shouldBe null
    edge1.propertiesMap.get("STR") shouldBe "<[empty]>"
    def node1Trav = graph.nodes(Node1.Label).asScala.cast[Node1]
    def edge1Trav = graph.edges(Edge1.Label).asScala.cast[Edge1]
    node1Trav.str.next() shouldBe "<[empty]>"
    node1Trav.intList.l shouldBe Seq.empty
    node1Trav.property(Node1.Properties.Str).next() shouldBe "<[empty]>"
    edge1Trav.property(Edge1.Properties.Str).next() shouldBe "<[empty]>"
  }

  "defined property values" in {
    val storageLocation = Files.createTempFile("overflowdb-codegen-tests", "odb.bin")

    def openGraph(): Graph = TestSchema.withStorage(storageLocation).graph
    val graph = openGraph()

    val node1 = graph.addNode(Node1.Label)
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]
    val properties = Seq(
      Properties.BOOL.of(false),
      Properties.STR.of("foo"),
      Properties.BYTE.of(100: Byte),
      Properties.SHORT.of(101: Short),
      Properties.INT.of(102),
      Properties.LONG.of(103),
      Properties.FLOAT1.of(Float.NaN),
      Properties.FLOAT2.of(104.4f),
      Properties.DOUBLE1.of(Double.NaN),
      Properties.DOUBLE2.of(105.5),
      Properties.CHAR.of('Z'),
      Properties.INT_LIST.of(ArraySeq(3, 4, 5)),
    )
    properties.foreach(node1.setProperty)
    properties.foreach(edge1.setProperty)
    node1.setProperty(Node1.Properties.Node1inner.of(node2))

    val node1Id = node1.id
    val node2Id = node2.id
    verifyValues(graph, node1Id, node2Id)
    graph.close()
    verifyValues(openGraph(), node1Id, node2Id)

    def verifyValues(graph: Graph, node1Id: Long, node2Id: Long): Unit = {
      val node1 = graph.node(node1Id).asInstanceOf[Node1]
      val node2 = graph.node(node2Id).asInstanceOf[Node1]
      node1.bool shouldBe false
      node1.str shouldBe "foo"
      node1.byte shouldBe 100
      node1.short shouldBe 101
      node1.int shouldBe 102
      node1.long shouldBe 103
      node1.float1.isNaN shouldBe true
      node1.float2 shouldBe 104.4f
      node1.double1.isNaN shouldBe true
      node1.double2 shouldBe 105.5
      node1.char shouldBe 'Z'
      node1.intList shouldBe Seq(3, 4, 5)
      node1.node1Inner shouldBe node2
      node1.propertyKeys().contains("STR") shouldBe true
      node1.propertyDefaultValue("STR") shouldBe "<[empty]>"
      node1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
      node1.property(Node1.Properties.Str) shouldBe "foo"
      node1.property(Node1.Properties.Node1inner) shouldBe node2
      node1.property("DOESNT_EXIST") shouldBe null
      node1.propertiesMap.get("STR") shouldBe "foo"
      node1.propertiesMap.get(Node1.PropertyNames.Node1inner) shouldBe node2
      node1.get.propertiesMapForStorage.get(PropertyNames.STR) shouldBe "foo"
      node1.get.propertiesMapForStorage.get(Node1.PropertyNames.Node1inner) shouldBe node2

      edge1.bool shouldBe false
      edge1.str shouldBe "foo"
      edge1.byte shouldBe 100
      edge1.short shouldBe 101
      edge1.int shouldBe 102
      edge1.long shouldBe 103
      edge1.float1.isNaN shouldBe true
      edge1.float2 shouldBe 104.4f
      edge1.double1.isNaN shouldBe true
      edge1.double2 shouldBe 105.5
      edge1.char shouldBe 'Z'
      edge1.intList shouldBe Seq(3, 4, 5)
      edge1.propertyKeys().contains("STR") shouldBe true
      edge1.propertyDefaultValue("STR") shouldBe "<[empty]>"
      edge1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
      edge1.property(Node1.Properties.Str) shouldBe "foo"
      edge1.property("DOESNT_EXIST") shouldBe null
      edge1.propertiesMap.get("STR") shouldBe "foo"

      def node1Trav = graph.nodes(Node1.Label).asScala.cast[Node1]
      def edge1Trav = graph.edges(Edge1.Label).asScala.cast[Edge1]

      node1Trav.str.next() shouldBe "foo"
      node1Trav.intList.l shouldBe Seq(3, 4, 5)
      node1Trav.property(Node1.Properties.Str).next() shouldBe "foo"
      edge1Trav.property(Edge1.Properties.Str).next() shouldBe "foo"
    }
  }

  "handle primitive arrays for list properties" in {
    def openGraph(): Graph = TestSchema.empty.graph
    val graph = openGraph()

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val node2 = graph.addNode(Node1.Label).asInstanceOf[Node1]
    val edge1 = node1.addEdge(Edge1.Label, node2).asInstanceOf[Edge1]
    node1.setProperty(Properties.INT_LIST.name, Array(1,2,3))
    edge1.setProperty(Properties.INT_LIST.name, Array(3,4,5))

    node1.intList shouldBe IndexedSeq(1,2,3)
    edge1.intList shouldBe IndexedSeq(3,4,5)
  }

  "NewNode.copy includes contained nodes" in {
    val node1 = NewNode1()
      .bool(true)
      .str("foo")
      .intList(Seq(1,2,3): Seq[Integer])

    val node2 = NewNode1().node1Inner(node1)

    val node1Copy = node1.copy
    node1Copy.bool shouldBe true
    node1Copy.str shouldBe "foo"
    node1Copy.intList shouldBe Seq(1,2,3)

    val node2Copy = node2.copy
    node2Copy.node1Inner shouldBe node1
  }

  "generated string property filters" in {
    val graph = TestSchema.empty.graph
    val node1 = graph.addNode(Node1.Label, Node1.PropertyNames.Str, "node1 name")
    val node2 = graph.addNode(Node1.Label, Node1.PropertyNames.Str,
      """node2 name line 1
        |node2 name line 2""")
    val node3 = graph.addNode(Node1.Label)
    def node1Traversal = graph.nodes(Node1.Label).asScala.cast[Node1]

    node1Traversal.size shouldBe 3
    node1Traversal.str(".*").size shouldBe 3
    node1Traversal.str(".*name.*").size shouldBe 2
    node1Traversal.str(".*NAME.*").size shouldBe 0 // case sensitive by default
    node1Traversal.str(".*(?i)NAME.*").size shouldBe 2 // case insensitive on request
    node1Traversal.str(".*node1.*").size shouldBe 1
    node1Traversal.str(".*line 2.*").size shouldBe 1 // testing multi line matcher
    node1Traversal.str("nomatch", ".*line 2.*").size shouldBe 1
    node1Traversal.strExact("node1 name").size shouldBe 1
    node1Traversal.strExact("nomatch", "node1 name").size shouldBe 1
    node1Traversal.strNot(".*node1.*").size shouldBe 2
    node1Traversal.strNot(".*line 2.*").size shouldBe 2 // testing multi line matcher
    node1Traversal.strNot("nomatch", ".*line 2.*").size shouldBe 2
  }

}
