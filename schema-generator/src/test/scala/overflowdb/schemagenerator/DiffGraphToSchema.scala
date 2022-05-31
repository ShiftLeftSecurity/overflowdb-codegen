package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

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
      )
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include(s"""val stringProp = builder.addProperty(name = "stringProp", valueType = ValueType.String)""")
    result should include(s"""val boolProp1 = builder.addProperty(name = "boolProp1", valueType = ValueType.Boolean)""")
    result should include(s"""val boolProp2 = builder.addProperty(name = "boolProp2", valueType = ValueType.Boolean)""")
    result should include(s"""val byteProp1 = builder.addProperty(name = "byteProp1", valueType = ValueType.Byte)""")
    result should include(s"""val byteProp2 = builder.addProperty(name = "byteProp2", valueType = ValueType.Byte)""")
    result should include("""val thing = builder.addNodeType(name = "Thing").addProperties(boolProp1, boolProp2, byteProp1, byteProp2, stringProp)""")
    result should include("""val thing = builder.addNodeType(name = "Thing").addProperties(boolProp1, boolProp2, byteProp1, byteProp2, stringProp)""")

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
