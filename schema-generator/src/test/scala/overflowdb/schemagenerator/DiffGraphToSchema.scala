package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "SampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  "simple schema with no ambiguities" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode("Artist", "name", "Bob Dylan")
      .addNode("Song", "length", 195, "original", true)
//      .addEdge()
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include(s"""val name = builder.addProperty(name = "name")""")
    result should include(s"""val length = builder.addProperty(name = "length")""")
    result should include(s"""val original = builder.addProperty(name = "original")""")
    result should include("""val artist = builder.addNodeType(name = "Artist").addProperties(name)""")
    result should include("""val song = builder.addNodeType(name = "Song").addProperties(length, original)""")

    // TODO rm
//    println(result)
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
