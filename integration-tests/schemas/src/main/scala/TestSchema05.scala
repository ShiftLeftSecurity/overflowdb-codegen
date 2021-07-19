import overflowdb.schema.Property._

/** testing all property types with Cardinality.ZeroOrOne */
class TestSchema05 extends TestSchema {
  val bool = builder.addProperty("BOOL", ValueType.Boolean)
  val string = builder.addProperty("STR", ValueType.String)
  val byte = builder.addProperty("BYTE", ValueType.Byte)
  val short = builder.addProperty("SHORT", ValueType.Short)
  val int  = builder.addProperty("INT", ValueType.Int)
  val long = builder.addProperty("LONG", ValueType.Long)
  val float = builder.addProperty("FLOAT", ValueType.Float)
  val double = builder.addProperty("DOUBLE", ValueType.Double)
  val char = builder.addProperty("CHAR", ValueType.Char)

  val node1 = builder.addNodeType("NODE1")
    .addProperties(bool, string, byte, short, int, long, float, double, char)

  val edge1 = builder.addEdgeType("EDGE1")
    .addProperties(bool, string, byte, short, int, long, float, double, char)

  node1.addOutEdge(edge1, node1)
}