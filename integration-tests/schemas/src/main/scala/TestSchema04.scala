import overflowdb.schema.Property.Default
import overflowdb.schema._

/** For testing default values on properties with Cardinality.One: we have type-dependent defaults,
  * and allow to override them in the schema. */
class TestSchema04 extends TestSchema {
  import Property.Cardinality
  val bool = builder.addProperty[Boolean]("BOOL", Cardinality.One(Default(true)))
  val string = builder.addProperty[String]("STR", Cardinality.One(Default("<[empty]>")))
  val byte = builder.addProperty[Byte]("BYTE", Cardinality.One(Default(1: Byte)))
  val short = builder.addProperty[Short]("SHORT", Cardinality.One(Default(2: Short)))
  val int  = builder.addProperty[Int]("INT", Cardinality.One(Default(3: Int)))
  val long = builder.addProperty[Long]("LONG", Cardinality.One(Default(4: Long)))
//  val float = builder.addProperty("FLOAT", ValueTypes.FLOAT, Cardinality.One)
//  val double = builder.addProperty("DOUBLE", ValueTypes.DOUBLE, Cardinality.One)
//  val char = builder.addProperty("CHAR", ValueTypes.CHARACTER, Cardinality.One)
//
  val node1 = builder.addNodeType("NODE1")
    .addProperties(bool, string, byte, short, int, long)//, float, double, char)
//
//  val edge1 = builder.addEdgeType("EDGE1")
//    .addProperties(bool, string, byte, short, int, long, float, double, char)
//
//  node1.addOutEdge(edge1, node1)
}
