import overflowdb.schema.Property.ValueType
import overflowdb.schema._

/** For testing base node type functionality, e.g. defining properties and edges on
  * base node types.
  */
class TestSchema02 extends TestSchema {
  val name = builder.addProperty("NAME", ValueType.String, "Name of represented object")
    .mandatory("<[empty]>")

  val order = builder.addProperty("ORDER", ValueType.Int, "General ordering property.")

  val baseNode = builder.addNodeBaseType(
    name = "BASE_NODE",
    comment = "base node"
  ).addProperty(name)

  val node1 = builder.addNodeType(
    name = "NODE1",
    comment = "sample node 1"
  ).addProperties(order)
    .extendz(baseNode)

  val node2 = builder.addNodeType(
    name = "NODE2",
    comment = "sample node 2"
  )

  val edge1 = builder.addEdgeType(
    name = "EDGE1",
    comment = "sample edge 1")

  val edge2 = builder.addEdgeType(
    name = "EDGE2",
    comment = "sample edge 2").addProperty(name)

  baseNode.addOutEdge(
    edge = edge1,
    inNode = node2,
    cardinalityOut = EdgeType.Cardinality.List,
    cardinalityIn = EdgeType.Cardinality.ZeroOrOne,
    stepNameOut = "customStepName1",
    stepNameOutDoc = "custom step name 1 documentation",
    stepNameIn = "customStepName1Inverse")

  node2.addOutEdge(
    edge = edge2,
    inNode = baseNode,
    cardinalityOut = EdgeType.Cardinality.One,
    cardinalityIn = EdgeType.Cardinality.One,
    stepNameOut = "customStepName2",
    stepNameOutDoc = "custom step name 2 documentation",
    stepNameIn = "customStepName2Inverse")
}
