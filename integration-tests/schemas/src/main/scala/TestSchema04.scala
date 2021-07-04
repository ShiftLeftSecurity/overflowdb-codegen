import overflowdb.schema._
import overflowdb.storage.ValueTypes

/** For testing default values on properties with Cardinality.One: we have type-dependent defaults,
  * and allow to override them in the schema. */
class TestSchema04 extends TestSchema {
  val bool = builder.addProperty("BOOL", ValueTypes.BOOLEAN, Cardinality.One)
  val string = builder.addProperty("STR", ValueTypes.STRING, Cardinality.One)
  val byte = builder.addProperty("BYTE", ValueTypes.BYTE, Cardinality.One)
  val short = builder.addProperty("SHORT", ValueTypes.SHORT, Cardinality.One)
  val int  = builder.addProperty("INT", ValueTypes.INTEGER, Cardinality.One)
  val long = builder.addProperty("LONG", ValueTypes.LONG, Cardinality.One)
  val float = builder.addProperty("FLOAT", ValueTypes.FLOAT, Cardinality.One)
  val double = builder.addProperty("DOUBLE", ValueTypes.DOUBLE, Cardinality.One)
  val char = builder.addProperty("CHAR", ValueTypes.CHARACTER, Cardinality.One)
  
  val node1 = builder.addNodeType("NODE1")
    .addProperties(bool, string, byte, short, int, long, float, double, char)

  val edge1 = builder.addEdgeType("EDGE1")
    .addProperties(bool, string, byte, short, int, long, float, double, char)

  node1.addOutEdge(edge1, node1)
}
