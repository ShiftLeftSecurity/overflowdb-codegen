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
  val float1 = builder.addProperty[Float]("FLOAT1", Cardinality.One(Default(5.5f)))
  val float2 = builder.addProperty[Float]("FLOAT2", Cardinality.One(Default(Float.NaN)))
  val double1 = builder.addProperty[Double]("DOUBLE1", Cardinality.One(Default(6.6)))
  val double2 = builder.addProperty[Double]("DOUBLE2", Cardinality.One(Default(Double.NaN)))
  val char = builder.addProperty[Char]("CHAR", Cardinality.One(Default('?')))

  val node1 = builder.addNodeType("NODE1")
    .addProperties(bool, string, byte, short, int, long, float1, float2, double1, double2, char)

//  val edge1 = builder.addEdgeType("EDGE1")
//    .addProperties(bool, string, byte, short, int, long, float1, float2, double1, double2, char)
//
//  node1.addOutEdge(edge1, node1)
}
