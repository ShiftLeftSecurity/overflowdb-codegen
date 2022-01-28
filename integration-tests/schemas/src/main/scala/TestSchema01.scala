import overflowdb.schema.Property.ValueType
import overflowdb.schema._

class TestSchema01 extends TestSchema {
  val name = builder.addProperty("NAME", ValueType.String, "Name of represented object")
    .mandatory("<[empty]>")

  val order = builder.addProperty("ORDER", ValueType.Int, "General ordering property.")

  val options = builder.addProperty("OPTIONS", ValueType.String, "Options of a node").asList()
  val placements = builder.addProperty("PLACEMENTS", ValueType.Int, "placements in some league").asList()

  val node1 = builder.addNodeType(
    name = "NODE1",
    comment = "sample node 1"
  ).addProperties(name, order)

  val node2 = builder.addNodeType(
    name = "NODE2",
    comment = "sample node 2"
  ).addProperties(name, options, placements)

  val node3 = builder.addNodeType(
    name = "NODE3",
    comment = "sample node 3"
  )

  node2.addContainedNode(
    node = node3,
    localName = "node3",
    cardinality = Property.Cardinality.ZeroOrOne,
    comment = "node 3 documentation foo bar")

  val edge1 = builder.addEdgeType(
    name = "EDGE1",
    comment = "sample edge 1")

  val edge2 = builder.addEdgeType(
    name = "EDGE2",
    comment = "sample edge 2")
  .addProperties(name, order, options, placements)

  node1.addOutEdge(
    edge = edge1,
    inNode = node2,
    cardinalityOut = EdgeType.Cardinality.List,
    cardinalityIn = EdgeType.Cardinality.ZeroOrOne)

  node2.addOutEdge(
    edge = edge2,
    inNode = node1,
    cardinalityOut = EdgeType.Cardinality.One,
    cardinalityIn = EdgeType.Cardinality.One)
}
