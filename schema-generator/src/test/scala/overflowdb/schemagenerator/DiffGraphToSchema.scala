package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

import scala.jdk.CollectionConverters.IterableHasAsJava

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "SampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  "simple schema" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode("Artist", "name", "Bob Dylan")
      .addNode("Song", "name", "The times they are a changin'")
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include(s"""val name = builder.addProperty(name = "name", valueType = ValueType.String)""")
    result should include("""val artist = builder.addNodeType(name = "Artist").addProperties(name)""")
    result should include("""val song = builder.addNodeType(name = "Song").addProperties(name)""")
  }

  "testing all property value types" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode("Thing",
        "stringProp", "stringValue",
        "boolProp1", true,
        "boolProp2", true: java.lang.Boolean,
        "byteProp1", 1: Byte,
        "byteProp2", java.lang.Byte.valueOf(2: Byte),
        "shortProp1", 3: Short,
        "shortProp2", java.lang.Short.valueOf(4: Short),
        "intProp1", 5: Int,
        "intProp2", 6: Integer,
        "longProp1", 7: Long,
        "longProp2", 8: java.lang.Long,
        "floatProp1", 9.1f: Float,
        "floatProp2", 10.1f: java.lang.Float,
        "doubleProp1", 11.1f: Double,
        "doubleProp2", 12.1f: java.lang.Double,
        "charProp1", 'a': Char,
        "charProp2", 'b': Character,
        "listProp1", Array("string1", "string2"),
        "listProp2", Seq("string1", "string2"),
        "listProp3", Seq("string1", "string2").asJava,
      )
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include(s"""val stringProp = builder.addProperty(name = "stringProp", valueType = ValueType.String)""")
    result should include(s"""val boolProp1 = builder.addProperty(name = "boolProp1", valueType = ValueType.Boolean)""")
    result should include(s"""val boolProp2 = builder.addProperty(name = "boolProp2", valueType = ValueType.Boolean)""")
    result should include(s"""val byteProp1 = builder.addProperty(name = "byteProp1", valueType = ValueType.Byte)""")
    result should include(s"""val byteProp2 = builder.addProperty(name = "byteProp2", valueType = ValueType.Byte)""")
    result should include(s"""val shortProp1 = builder.addProperty(name = "shortProp1", valueType = ValueType.Short)""")
    result should include(s"""val shortProp2 = builder.addProperty(name = "shortProp2", valueType = ValueType.Short)""")
    result should include(s"""val intProp1 = builder.addProperty(name = "intProp1", valueType = ValueType.Int)""")
    result should include(s"""val intProp2 = builder.addProperty(name = "intProp2", valueType = ValueType.Int)""")
    result should include(s"""val longProp1 = builder.addProperty(name = "longProp1", valueType = ValueType.Long)""")
    result should include(s"""val longProp2 = builder.addProperty(name = "longProp2", valueType = ValueType.Long)""")
    result should include(s"""val floatProp1 = builder.addProperty(name = "floatProp1", valueType = ValueType.Float)""")
    result should include(s"""val floatProp2 = builder.addProperty(name = "floatProp2", valueType = ValueType.Float)""")
    result should include(s"""val doubleProp1 = builder.addProperty(name = "doubleProp1", valueType = ValueType.Double)""")
    result should include(s"""val doubleProp2 = builder.addProperty(name = "doubleProp2", valueType = ValueType.Double)""")
    result should include(s"""val charProp1 = builder.addProperty(name = "charProp1", valueType = ValueType.Char)""")
    result should include(s"""val charProp2 = builder.addProperty(name = "charProp2", valueType = ValueType.Char)""")
    result should include(s"""val listProp1 = builder.addProperty(name = "listProp1", valueType = ValueType.List)""")
    result should include(s"""val listProp2 = builder.addProperty(name = "listProp2", valueType = ValueType.List)""")
    result should include(s"""val listProp3 = builder.addProperty(name = "listProp3", valueType = ValueType.List)""")
    result should include("""val thing = builder.addNodeType(name = "Thing").addProperties(boolProp1, boolProp2, byteProp1, byteProp2, charProp1, charProp2, doubleProp1, doubleProp2, floatProp1, floatProp2, intProp1, intProp2, listProp1, listProp2, listProp3, longProp1, longProp2, shortProp1, shortProp2, stringProp)""")
  }

//  "schema with ambiguities in property names from diffgraph" in {
//    val diffGraph = new DiffGraphBuilder()
//      .addNode("Artist")
//      .addNode("Artist", "name", "Bob Dylan")
//      .addNode("Song")
//      .addNode("Song", "name", "The times they are a changin'")
//      .build()
//
//    val result = builder.build(diffGraph)
//
//    result should include("""val artist = builder.addNodeType(name = "Artist")""")
//    result should include("""val song = builder.addNodeType(name = "Song")""")
//
//    /* Properties are defined globally and can be reused across nodes and edges, but when generating a schema based on
//     * a diffgraph, we can't semantically derive simply based on it's name that it's the same property. */
//    result should include(s"""val nodeArtistPropertyName = builder.addProperty(name = "$PropertyY")""")
//  }
//
}
