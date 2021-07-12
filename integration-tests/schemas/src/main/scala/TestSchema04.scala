import overflowdb.schema.Property._
//import overflowdb.schema._

/** For testing default values on properties with Cardinality.One: we have type-dependent defaults,
  * and allow to override them in the schema. */
class TestSchema04 extends TestSchema {
  val bool = builder.addProperty("BOOL", ValueType.Boolean).mandatory(default = true)
  val string = builder.addProperty("STR", ValueType.String).mandatory(default = "<[empty]>")
  val byte = builder.addProperty("BYTE", ValueType.Byte).mandatory(default = 1)
  val short = builder.addProperty("SHORT", ValueType.Short).mandatory(default = 2)
  val int  = builder.addProperty("INT", ValueType.Int).mandatory(default = 3)
  val long = builder.addProperty("LONG", ValueType.Long).mandatory(default = 4)
  val float1 = builder.addProperty("FLOAT1", ValueType.Float).mandatory(default = 5.5f)
  val float2 = builder.addProperty("FLOAT2", ValueType.Float).mandatory(default = Float.NaN)
  val double1 = builder.addProperty("DOUBLE1", ValueType.Double).mandatory(default = 6.6)
  val double2 = builder.addProperty("DOUBLE2", ValueType.Double).mandatory(default = Double.NaN)
  val char = builder.addProperty("CHAR", ValueType.Char).mandatory(default = '?')

  val node1 = builder.addNodeType("NODE1")
    .addProperties(bool, string, byte, short, int, long, float1, float2, double1, double2, char)

  val edge1 = builder.addEdgeType("EDGE1")
    .addProperties(bool, string, byte, short, int, long, float1, float2, double1, double2, char)

  node1.addOutEdge(edge1, node1)
}
