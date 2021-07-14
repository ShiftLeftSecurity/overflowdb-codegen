import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.traversal._
import overflowdb.{Config, Graph}
import testschema04._
import testschema04.edges._
import testschema04.nodes._
import testschema04.traversal._

class Schema04Test extends AnyWordSpec with Matchers {

  "default property values" in {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

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
    node1.intListIndexed.size shouldBe 0
    node1.propertyKeys().contains("STR") shouldBe true
    node1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    node1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    node1.property(Node1.Properties.Str) shouldBe "<[empty]>"
    node1.property("DOESNT_EXIST") shouldBe null
    node1.propertiesMap.get("STR") shouldBe "<[empty]>"
    node1.get.propertiesMapWithoutDefaults.isEmpty shouldBe true

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
    edge1.intListIndexed.size shouldBe 0
    edge1.propertyKeys().contains("STR") shouldBe true
    edge1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    edge1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    edge1.property(Edge1.Properties.Str) shouldBe "<[empty]>"
    edge1.property("DOESNT_EXIST") shouldBe null
    edge1.propertiesMap.get("STR") shouldBe "<[empty]>"

    def node1Trav = graph.nodes(Node1.Label).cast[Node1]
    def edge1Trav = graph.edges(Edge1.Label).cast[Edge1]
    node1Trav.str.head shouldBe "<[empty]>"
    node1Trav.intList.l shouldBe Seq.empty
    node1Trav.property(Node1.Properties.Str).head shouldBe "<[empty]>"
    edge1Trav.property(Edge1.Properties.Str).head shouldBe "<[empty]>"
  }

  "defined property values" in {
    val graph = Graph.open(Config.withDefaults, nodes.Factories.allAsJava, edges.Factories.allAsJava)

    val node1 = graph.addNode(Node1.Label).asInstanceOf[Node1]
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
      // Properties.INT_LIST, ,
      // Properties.INT_LIST_INDEXED, ,
    )
    properties.foreach(node1.setProperty)
    properties.foreach(edge1.setProperty)

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
//    node1.intList.size shouldBe 0
//    node1.intListIndexed.size shouldBe 0
    node1.propertyKeys().contains("STR") shouldBe true
    node1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    node1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    node1.property(Node1.Properties.Str) shouldBe "foo"
    node1.property("DOESNT_EXIST") shouldBe null
    node1.propertiesMap.get("STR") shouldBe "foo"
    node1.get.propertiesMapWithoutDefaults.get(PropertyNames.STR) shouldBe "foo"

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
    //    edge1.intList.size shouldBe 0
    //    edge1.intListIndexed.size shouldBe 0
    edge1.propertyKeys().contains("STR") shouldBe true
    edge1.propertyDefaultValue("STR") shouldBe "<[empty]>"
    edge1.propertyDefaultValue("DOESNT_EXIST") shouldBe null
    edge1.property(Node1.Properties.Str) shouldBe "foo"
    edge1.property("DOESNT_EXIST") shouldBe null
    edge1.propertiesMap.get("STR") shouldBe "foo"

    def node1Trav = graph.nodes(Node1.Label).cast[Node1]
    def edge1Trav = graph.edges(Edge1.Label).cast[Edge1]
    node1Trav.str.head shouldBe "foo"
    node1Trav.intList.l shouldBe Seq.empty //TODO
    node1Trav.property(Node1.Properties.Str).head shouldBe "foo"
    edge1Trav.property(Edge1.Properties.Str).head shouldBe "foo"
  }
}
