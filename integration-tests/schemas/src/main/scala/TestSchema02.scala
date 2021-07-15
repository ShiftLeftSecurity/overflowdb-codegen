import overflowdb.schema.Property.ValueType
import overflowdb.schema._

/** For testing base node type functionality, e.g. defining properties and edges on
  * base node types.
  */
class TestSchema02 extends TestSchema {
  val name = builder.addProperty("NAME", ValueType.String, "Name of represented object")
    .mandatory("<[empty]>")

  val order = builder.addProperty("ORDER", ValueType.Int, "General ordering property.")

  val node1Base = builder.addNodeBaseType(
    name = "BASE_NODE",
    comment = "base node"
  ).addProperty(name)

  val node1 = builder.addNodeType(
    name = "NODE1",
    comment = "sample node 1"
  ).addProperties(order)
    .extendz(node1Base)

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

  node1Base.addOutEdge(
    edge = edge1,
    inNode = node2,
    cardinalityOut = EdgeType.Cardinality.List,
    cardinalityIn = EdgeType.Cardinality.ZeroOrOne)

  node2.addOutEdge(
    edge = edge2,
    inNode = node1Base,
    cardinalityOut = EdgeType.Cardinality.One,
    cardinalityIn = EdgeType.Cardinality.One)
}
