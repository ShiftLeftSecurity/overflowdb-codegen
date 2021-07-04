import overflowdb.schema._
import overflowdb.storage.ValueTypes

/** For testing default values on properties with Cardinality.One: we have type-dependent defaults,
  * and allow to override them in the schema. */
class TestSchema04 extends TestSchema {
  val stringP = builder.addProperty("STR", ValueTypes.STRING, Cardinality.One)

  val node1 = builder.addNodeType("NODE1")
    .addProperties(stringP)

  val edge1 = builder.addEdgeType("EDGE1")
    .addProperties(stringP)

  node1.addOutEdge(edge1, node1)
}
