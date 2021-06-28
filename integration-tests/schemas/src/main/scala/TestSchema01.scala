import overflowdb.schema._
import overflowdb.storage.ValueTypes

class TestSchema01 extends TestSchema {
  val name = builder.addProperty(
    name = "NAME",
    valueType = ValueTypes.STRING,
    cardinality = Cardinality.One,
    comment = "Name of represented object")

  val order = builder.addProperty(
    name = "ORDER",
    valueType = ValueTypes.INTEGER,
    cardinality = Cardinality.ZeroOrOne,
    comment = "General ordering property.")

  val options = builder.addProperty(
    name = "OPTIONS",
    valueType = ValueTypes.STRING,
    cardinality = Cardinality.List,
    comment = "Options of a node")

  val placements = builder.addProperty(
    name = "PLACEMENTS",
    valueType = ValueTypes.INTEGER,
    cardinality = Cardinality.ISeq,
    comment = "placements in some league")

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

  node2.addContainedNode(node3, "node3", Cardinality.ZeroOrOne)

  val edge1 = builder.addEdgeType(
    name = "EDGE1",
    comment = "sample edge 1")

  val edge2 = builder.addEdgeType(
    name = "EDGE2",
    comment = "sample edge 2").addProperty(name)

  node1.addOutEdge(
    edge = edge1,
    inNode = node2,
    cardinalityOut = Cardinality.List,
    cardinalityIn = Cardinality.ZeroOrOne)

  node2.addOutEdge(
    edge = edge2,
    inNode = node1,
    cardinalityOut = Cardinality.One,
    cardinalityIn = Cardinality.One)
}
