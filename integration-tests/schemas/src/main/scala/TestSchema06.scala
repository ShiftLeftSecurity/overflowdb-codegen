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

}
