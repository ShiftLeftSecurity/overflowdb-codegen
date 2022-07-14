import overflowdb.schema.Property.ValueType
import overflowdb.schema._

/** tests related to `AnyNode` */
class TestSchema06 extends TestSchema {
  val name = builder.addProperty("NAME", ValueType.String, "Name of represented object")
  val node1 = builder.addNodeType("NODE1").addProperty(name)
  val node2 = builder.addNodeType("NODE2")

  node2.addContainedNode(
    node = builder.anyNode,
    localName = "containedAnyNode",
    cardinality = Property.Cardinality.ZeroOrOne)

  val edge1 = builder.addEdgeType("edge1")
  val edge2 = builder.addEdgeType("edge2")

  node1.addOutEdge(
    edge = edge1,
    inNode = builder.anyNode,
    stepNameOut = "edge1OutNamed",
    stepNameIn = "edge1InNamed")

  builder.anyNode.addOutEdge(
    edge = edge2,
    inNode = node2,
    stepNameOut = "edge2OutNamed",
    stepNameIn = "edge2InNamed")
}
